package com.orbitalnet.core;

import java.util.List;
import java.util.Objects;

/**
 * Service engine responsible for executing avoidance maneuver replay.
 *
 * <p>Supports both ongoing and retrospective maneuver replays from a completed
 * baseline simulation without mutating the original baseline encounter or run.
 *
 * <p>Flow:
 * <ol>
 *   <li>Validates the replay input and maneuver candidate.</li>
 *   <li>Creates a separate {@link SimulationRun} referencing {@code baselineRunId}
 *       and {@code maneuverCandidateId} with independent object instances reconstructed
 *       from the scenario's initial conditions.</li>
 *   <li>Replays simulation progression through {@code detectedAtTick}.</li>
 *   <li>Applies the candidate's delta-v impulse to the target object's pre-maneuver velocity.</li>
 *   <li>Continues the remaining forecast over the full 3,600-second horizon using the 5-second timestep.</li>
 *   <li>Recomputes close approaches, collision conditions, and risk scores for the new run.</li>
 *   <li>Returns a {@link ManeuverReplayResult} capturing the separate replay outcome
 *       while preserving the original baseline encounter unchanged.</li>
 * </ol>
 */
public final class ManeuverReplayService {

    public ManeuverReplayService() {
    }

    /**
     * Executes retrospective maneuver replay using an immutable {@link ManeuverReplayInput}.
     *
     * @param newRunId unique identifier for the new maneuver run
     * @param replayInput immutable input capturing the baseline replay context
     * @param candidate the selected maneuver candidate to apply
     * @return the result of the replay including the new run and recalculated encounters
     */
    public ManeuverReplayResult replayAndApply(
            String newRunId,
            ManeuverReplayInput replayInput,
            ManeuverCandidate candidate) {

        return replayAndApply(
                newRunId,
                replayInput,
                candidate,
                new EncounterDetectionService(),
                new RiskEngine()
        );
    }

    /**
     * Executes retrospective maneuver replay with caller-provided detection and risk services.
     */
    public ManeuverReplayResult replayAndApply(
            String newRunId,
            ManeuverReplayInput replayInput,
            ManeuverCandidate candidate,
            EncounterDetectionService detectionService,
            RiskEngine riskEngine) {

        validateReplayInputAndCandidate(newRunId, replayInput, candidate);
        Objects.requireNonNull(detectionService, "Detection service cannot be null.");
        Objects.requireNonNull(riskEngine, "Risk engine cannot be null.");

        // 1. Create a separate SimulationRun with fresh object copies reconstructed from scenario initial states
        SimulationRun maneuverRun = new SimulationRun(
                newRunId,
                replayInput.getScenario(),
                replayInput.getBaselineRunId(),
                candidate.getManeuverCandidateId()
        );

        EncounterTracker tracker = new EncounterTracker(newRunId);
        int detectionTick = replayInput.getDetectedAtTick();
        boolean deltaVApplied = false;

        maneuverRun.start();

        // 2. Simulate tick progression from tick 0 through horizon (720 ticks / 3600 seconds)
        while (maneuverRun.getStatus() == SimulationRun.Status.RUNNING) {
            int currentTick = maneuverRun.getCurrentTick();

            // Detect and track encounters at current tick
            List<EncounterDetectionService.DetectedEncounter> detections =
                    detectionService.detect(maneuverRun.getObjects());

            List<Encounter> updatedEncounters =
                    tracker.processTick(currentTick, detections, maneuverRun.getObjects());

            // Assess newly detected encounters with the shared RiskEngine
            for (Encounter e : updatedEncounters) {
                if (e.getStatus() == Encounter.Status.DETECTED) {
                    e.assess(riskEngine.assess(e));
                }
            }

            // Apply candidate delta-v at the detection tick to the target's pre-maneuver velocity
            if (currentTick == detectionTick && !deltaVApplied) {
                SpaceObject targetObject = findObjectById(
                        maneuverRun.getObjects(), candidate.getTargetObjectId());

                if (targetObject == null || !targetObject.isActive()) {
                    maneuverRun.fail();
                    throw new IllegalStateException(
                            "Target object " + candidate.getTargetObjectId()
                                    + " was not found or is inactive in maneuver run at tick " + currentTick);
                }

                Vector3D newVelocity = targetObject.getVelocity().add(candidate.getDeltaV());
                targetObject.setVelocity(newVelocity);
                deltaVApplied = true;
            }

            maneuverRun.advanceTick();
        }

        // Final detection pass at completion tick if completed
        if (maneuverRun.getStatus() == SimulationRun.Status.COMPLETED) {
            int finalTick = maneuverRun.getCurrentTick();
            List<EncounterDetectionService.DetectedEncounter> finalDetections =
                    detectionService.detect(maneuverRun.getObjects());
            tracker.processTick(finalTick, finalDetections, maneuverRun.getObjects());
        }

        return new ManeuverReplayResult(
                newRunId,
                replayInput.getBaselineRunId(),
                candidate.getManeuverCandidateId(),
                maneuverRun,
                tracker.getEncounters(),
                detectionTick,
                candidate.getDeltaV(),
                candidate.getTargetObjectId()
        );
    }

    /**
     * Legacy adapter overload supporting replay from a baseline run and encounter.
     * Delegates to the non-mutating {@link ManeuverReplayInput} replay workflow.
     */
    public ManeuverReplayResult replayAndApply(
            String newRunId,
            SimulationRun baselineRun,
            Encounter encounter,
            ManeuverCandidate candidate) {

        return replayAndApply(
                newRunId,
                baselineRun,
                encounter,
                candidate,
                new EncounterDetectionService(),
                new RiskEngine()
        );
    }

    /**
     * Legacy adapter overload with caller-provided detection and risk services.
     */
    public ManeuverReplayResult replayAndApply(
            String newRunId,
            SimulationRun baselineRun,
            Encounter encounter,
            ManeuverCandidate candidate,
            EncounterDetectionService detectionService,
            RiskEngine riskEngine) {

        validateLegacyInputs(newRunId, baselineRun, encounter, candidate);

        String targetId = candidate.getTargetObjectId();
        String otherId = targetId.equals(encounter.getObjectAId())
                ? encounter.getObjectBId()
                : encounter.getObjectAId();

        ManeuverReplayInput replayInput = new ManeuverReplayInput(
                baselineRun.getRunId(),
                baselineRun.getScenario(),
                encounter.getEncounterId(),
                encounter.getDetectedAtTick(),
                targetId,
                otherId,
                encounter.getMinSeparationKm(),
                encounter.getTimeToClosestApproachSeconds(),
                encounter.getRelativeVelocityKmPerSec(),
                encounter.getRiskScore()
        );

        return replayAndApply(newRunId, replayInput, candidate, detectionService, riskEngine);
    }

    private static void validateReplayInputAndCandidate(
            String newRunId,
            ManeuverReplayInput replayInput,
            ManeuverCandidate candidate) {

        if (newRunId == null || newRunId.isBlank()) {
            throw new IllegalArgumentException("New run ID cannot be blank.");
        }
        Objects.requireNonNull(replayInput, "Replay input cannot be null.");
        Objects.requireNonNull(candidate, "Maneuver candidate cannot be null.");

        if (newRunId.equals(replayInput.getBaselineRunId())) {
            throw new IllegalArgumentException(
                    "Maneuver run ID must be distinct from baseline run ID: " + newRunId);
        }

        if (!candidate.getEncounterId().equals(replayInput.getEncounterId())) {
            throw new IllegalArgumentException(
                    "Candidate encounter ID '" + candidate.getEncounterId()
                            + "' does not match replay input encounter ID '" + replayInput.getEncounterId() + "'");
        }

        if (!candidate.getTargetObjectId().equals(replayInput.getTargetObjectId())) {
            throw new IllegalArgumentException(
                    "Candidate target object ID '" + candidate.getTargetObjectId()
                            + "' does not match replay input target object ID '" + replayInput.getTargetObjectId() + "'");
        }
    }

    private static void validateLegacyInputs(
            String newRunId,
            SimulationRun baselineRun,
            Encounter encounter,
            ManeuverCandidate candidate) {

        if (newRunId == null || newRunId.isBlank()) {
            throw new IllegalArgumentException("New run ID cannot be blank.");
        }
        Objects.requireNonNull(baselineRun, "Baseline run cannot be null.");
        Objects.requireNonNull(encounter, "Encounter cannot be null.");
        Objects.requireNonNull(candidate, "Maneuver candidate cannot be null.");

        if (newRunId.equals(baselineRun.getRunId())) {
            throw new IllegalArgumentException(
                    "Maneuver run ID must be distinct from baseline run ID: " + newRunId);
        }

        if (baselineRun.getStatus() == SimulationRun.Status.FAILED) {
            throw new IllegalArgumentException(
                    "Cannot replay a maneuver from a failed baseline run.");
        }

        if (!candidate.getEncounterId().equals(encounter.getEncounterId())) {
            throw new IllegalArgumentException(
                    "Candidate encounter ID '" + candidate.getEncounterId()
                            + "' does not match encounter '" + encounter.getEncounterId() + "'");
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

        int tick = encounter.getDetectedAtTick();
        int maxTicks = (int) (SimulationConstants.SIMULATION_HORIZON_SECONDS
                / SimulationConstants.DELTA_TIME_SECONDS);

        if (tick < 0 || tick > maxTicks) {
            throw new IllegalArgumentException(
                    "Encounter detection tick " + tick + " is outside simulation horizon [0, "
                            + maxTicks + "]");
        }

        String targetId = candidate.getTargetObjectId();
        boolean matchesA = targetId.equals(encounter.getObjectAId());
        boolean matchesB = targetId.equals(encounter.getObjectBId());

        if (!matchesA && !matchesB) {
            throw new IllegalArgumentException(
                    "Target object '" + targetId + "' does not belong to encounter "
                            + encounter.getEncounterId());
        }
    }

    private static SpaceObject findObjectById(List<SpaceObject> objects, String id) {
        for (SpaceObject obj : objects) {
            if (obj.getId().equals(id)) {
                return obj;
            }
        }
        return null;
    }
}
