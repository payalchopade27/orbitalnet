package com.orbitalnet.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Public facade and primary entry point for Member 2's service layer to consume the
 * OrbitalNet core simulation engine.
 *
 * <p>Orchestrates baseline simulation runs, encounter detection/tracking, risk scoring,
 * threat ranking via {@link CustomMaxHeap}, threat clustering via {@link OrbitalGraph},
 * maneuver planning, separate retrospective maneuver replay via {@link ManeuverReplayService},
 * and benchmarking via {@link SpatialGridBenchmark}.
 *
 * <p>Remains completely independent of Spring Boot, Jackson, HTTP, and database concerns.
 */
public final class OrbitalEngine {

    private final ScenarioGenerator scenarioGenerator;
    private final EncounterDetectionService detectionService;
    private final RiskEngine riskEngine;
    private final ManeuverPlanner maneuverPlanner;
    private final ManeuverReplayService replayService;

    public OrbitalEngine() {
        this(
                new ScenarioGenerator(),
                new EncounterDetectionService(),
                new RiskEngine(),
                new ManeuverPlanner(),
                new ManeuverReplayService()
        );
    }

    public OrbitalEngine(
            ScenarioGenerator scenarioGenerator,
            EncounterDetectionService detectionService,
            RiskEngine riskEngine,
            ManeuverPlanner maneuverPlanner,
            ManeuverReplayService replayService) {
        this.scenarioGenerator = Objects.requireNonNull(scenarioGenerator, "Scenario generator cannot be null.");
        this.detectionService = Objects.requireNonNull(detectionService, "Detection service cannot be null.");
        this.riskEngine = Objects.requireNonNull(riskEngine, "Risk engine cannot be null.");
        this.maneuverPlanner = Objects.requireNonNull(maneuverPlanner, "Maneuver planner cannot be null.");
        this.replayService = Objects.requireNonNull(replayService, "Maneuver replay service cannot be null.");
    }

    /**
     * Generates a deterministic space traffic scenario using the specified configuration and seed.
     */
    public Scenario generateScenario(String scenarioId, ScenarioGenerationConfig config, long seed) {
        return scenarioGenerator.generate(scenarioId, config, seed);
    }

    /**
     * Generates the reproducible baseline known-threat scenario.
     */
    public Scenario generateKnownThreatScenario(String scenarioId, long seed) {
        return scenarioGenerator.generateKnownThreatScenario(scenarioId, seed);
    }

    /**
     * Executes a baseline simulation run from tick 0 through the 3,600-second horizon with
     * broad/narrow encounter detection, continuity tracking, and explainable risk assessment.
     *
     * @param runId unique identifier for this simulation run
     * @param scenario scenario providing initial conditions
     * @return complete execution outcome including run state, encounters, ranked threats, and graph
     */
    public SimulationExecutionResult executeBaselineRun(String runId, Scenario scenario) {
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("Run ID cannot be blank.");
        }
        Objects.requireNonNull(scenario, "Scenario cannot be null.");

        SimulationRun run = new SimulationRun(runId, scenario, null, null);
        EncounterTracker tracker = new EncounterTracker(runId);
        run.start();

        try {
            while (run.getStatus() == SimulationRun.Status.RUNNING) {
                int currentTick = run.getCurrentTick();

                List<EncounterDetectionService.DetectedEncounter> detections =
                        detectionService.detect(run.getObjects());

                List<Encounter> updated =
                        tracker.processTick(currentTick, detections, run.getObjects());

                for (Encounter e : updated) {
                    if (e.getStatus() == Encounter.Status.DETECTED) {
                        e.assess(riskEngine.assess(e));
                    }
                }

                run.advanceTick();
            }

            if (run.getStatus() == SimulationRun.Status.COMPLETED) {
                int finalTick = run.getCurrentTick();
                List<EncounterDetectionService.DetectedEncounter> finalDetections =
                        detectionService.detect(run.getObjects());
                tracker.processTick(finalTick, finalDetections, run.getObjects());
            }
        } catch (RuntimeException e) {
            if (run.getStatus() == SimulationRun.Status.RUNNING) {
                run.fail();
            }
            throw e;
        }

        List<Encounter> encounters = tracker.getEncounters();
        List<Encounter> rankedThreats = rankThreats(encounters);
        OrbitalGraph graph = buildEncounterGraph(encounters);

        return new SimulationExecutionResult(
                runId,
                scenario.getScenarioId(),
                run,
                encounters,
                rankedThreats,
                graph
        );
    }

    /**
     * Ranks encounters in descending order of risk using the custom {@link CustomMaxHeap}.
     */
    public List<Encounter> rankThreats(List<Encounter> encounters) {
        if (encounters == null || encounters.isEmpty()) {
            return Collections.emptyList();
        }

        CustomMaxHeap heap = new CustomMaxHeap(encounters.size());
        for (Encounter encounter : encounters) {
            heap.insert(encounter);
        }

        List<Encounter> ranked = new ArrayList<>(heap.size());
        while (!heap.isEmpty()) {
            ranked.add(heap.extractMax());
        }

        return Collections.unmodifiableList(ranked);
    }

    /**
     * Builds a threat relationship graph using {@link OrbitalGraph} from close-approach encounters.
     */
    public OrbitalGraph buildEncounterGraph(List<Encounter> encounters) {
        return OrbitalGraph.fromEncounters(encounters);
    }

    /**
     * Creates an immutable {@link ManeuverReplayInput} DTO from a baseline run and encounter.
     *
     * <p>Validates that the encounter belongs to the supplied baseline run and that the
     * chosen target object is one of the encounter's two objects. Does not require the
     * baseline encounter to be currently in {@code ASSESSED} status, enabling retrospective replay.
     *
     * @param baselineRun the baseline simulation run
     * @param encounter the encounter to be mitigated
     * @param targetObjectId the identifier of the object executing the avoidance maneuver
     * @return immutable replay input for subsequent planning and execution
     */
    public ManeuverReplayInput createReplayInput(
            SimulationRun baselineRun,
            Encounter encounter,
            String targetObjectId) {

        Objects.requireNonNull(baselineRun, "Baseline run cannot be null.");
        Objects.requireNonNull(encounter, "Encounter cannot be null.");
        if (targetObjectId == null || targetObjectId.isBlank()) {
            throw new IllegalArgumentException("Target object ID cannot be blank.");
        }

        if (baselineRun.getStatus() == SimulationRun.Status.FAILED) {
            throw new IllegalArgumentException("Cannot create replay input from a failed baseline run.");
        }

        if (!baselineRun.getRunId().equals(encounter.getRunId())) {
            throw new IllegalArgumentException(
                    "Encounter run ID '" + encounter.getRunId()
                            + "' does not match baseline run ID '" + baselineRun.getRunId() + "'");
        }

        if (encounter.getStatus() == Encounter.Status.DETECTED) {
            throw new IllegalStateException(
                    "Encounter must be assessed before a maneuver can be planned and replayed, but was: "
                            + encounter.getStatus());
        }

        boolean matchesA = targetObjectId.equals(encounter.getObjectAId());
        boolean matchesB = targetObjectId.equals(encounter.getObjectBId());
        if (!matchesA && !matchesB) {
            throw new IllegalArgumentException(
                    "Target object '" + targetObjectId + "' does not belong to encounter "
                            + encounter.getEncounterId());
        }

        String otherObjectId = matchesA ? encounter.getObjectBId() : encounter.getObjectAId();

        return new ManeuverReplayInput(
                baselineRun.getRunId(),
                baselineRun.getScenario(),
                encounter.getEncounterId(),
                encounter.getDetectedAtTick(),
                targetObjectId,
                otherObjectId,
                encounter.getMinSeparationKm(),
                encounter.getTimeToClosestApproachSeconds(),
                encounter.getRelativeVelocityKmPerSec(),
                encounter.getRiskScore()
        );
    }

    /**
     * Creates an immutable {@link ManeuverReplayInput} DTO from a baseline execution result.
     */
    public ManeuverReplayInput createReplayInput(
            SimulationExecutionResult baselineResult,
            Encounter encounter,
            String targetObjectId) {

        Objects.requireNonNull(baselineResult, "Baseline execution result cannot be null.");
        return createReplayInput(baselineResult.getSimulationRun(), encounter, targetObjectId);
    }

    /**
     * Generates 3 deterministic, orthogonal impulsive avoidance maneuver candidates.
     */
    public List<ManeuverCandidate> planManeuvers(
            Encounter encounter,
            SpaceObject target,
            SpaceObject other) {
        return maneuverPlanner.planManeuvers(encounter, target, other);
    }

    /**
     * Generates 3 deterministic, orthogonal impulsive avoidance maneuver candidates with custom magnitude.
     */
    public List<ManeuverCandidate> planManeuvers(
            Encounter encounter,
            SpaceObject target,
            SpaceObject other,
            double deltaVMagnitudeKmS) {
        return maneuverPlanner.planManeuvers(encounter, target, other, deltaVMagnitudeKmS);
    }

    /**
     * Generates 3 deterministic candidate maneuvers using the detection-time state reconstructed
     * from a {@link ManeuverReplayInput}.
     */
    public List<ManeuverCandidate> planManeuvers(ManeuverReplayInput replayInput) {
        return planManeuvers(replayInput, ManeuverPlanner.DEFAULT_DELTA_V_MAGNITUDE_KM_S);
    }

    /**
     * Generates 3 deterministic candidate maneuvers using detection-time state with custom impulse magnitude.
     */
    public List<ManeuverCandidate> planManeuvers(ManeuverReplayInput replayInput, double deltaVMagnitudeKmS) {
        Objects.requireNonNull(replayInput, "Replay input cannot be null.");
        SpaceObject target = replayInput.reconstructTargetObjectAtDetectionTick();
        SpaceObject other = replayInput.reconstructOtherObjectAtDetectionTick();

        Encounter contextEncounter = new Encounter(
                replayInput.getEncounterId(),
                replayInput.getBaselineRunId(),
                replayInput.getTargetObjectId(),
                replayInput.getOtherObjectId(),
                replayInput.getDetectionTimeMinSeparationKm(),
                replayInput.getDetectionTimeTimeToClosestApproachSeconds(),
                replayInput.getDetectionTimeRelativeVelocityKmPerSec(),
                replayInput.getDetectedAtTick()
        );
        if (replayInput.getDetectionTimeRiskScore() != null) {
            contextEncounter.assess(replayInput.getDetectionTimeRiskScore());
        }

        return maneuverPlanner.planManeuvers(contextEncounter, target, other, deltaVMagnitudeKmS);
    }

    /**
     * Applies a chosen maneuver candidate in a separate simulation run using an immutable
     * {@link ManeuverReplayInput}, preserving the baseline run and encounter unchanged.
     */
    public ManeuverReplayResult executeManeuverReplay(
            String newRunId,
            ManeuverReplayInput replayInput,
            ManeuverCandidate candidate) {
        return replayService.replayAndApply(
                newRunId,
                replayInput,
                candidate,
                detectionService,
                riskEngine
        );
    }

    /**
     * Legacy adapter overload supporting maneuver replay from a baseline run and encounter.
     * Safely constructs a {@link ManeuverReplayInput} and delegates without mutating the original encounter.
     */
    public ManeuverReplayResult executeManeuverReplay(
            String newRunId,
            SimulationRun baselineRun,
            Encounter encounter,
            ManeuverCandidate candidate) {
        Objects.requireNonNull(candidate, "Maneuver candidate cannot be null.");
        ManeuverReplayInput replayInput = createReplayInput(
                baselineRun, encounter, candidate.getTargetObjectId());
        return executeManeuverReplay(newRunId, replayInput, candidate);
    }

    /**
     * Runs spatial-grid benchmarking against naive pairwise checking for a specific object count.
     */
    public SpatialGridBenchmark.BenchmarkResult runBenchmark(int objectCount, long seed) {
        return SpatialGridBenchmark.runBenchmark(objectCount, seed);
    }

    /**
     * Runs spatial-grid benchmarking across multiple target object counts.
     */
    public List<SpatialGridBenchmark.BenchmarkResult> runAllBenchmarks(int[] objectCounts, long seed) {
        return SpatialGridBenchmark.runAll(objectCounts, seed);
    }
}
