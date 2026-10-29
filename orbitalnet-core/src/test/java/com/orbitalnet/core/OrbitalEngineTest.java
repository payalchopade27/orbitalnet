package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OrbitalEngineTest {

    @Test
    void executesBaselineRunSuccessfullyWithEncountersAndThreatRanking() {
        OrbitalEngine engine = new OrbitalEngine();
        Scenario scenario = engine.generateKnownThreatScenario("SCENARIO-KT", 42L);

        SimulationExecutionResult result = engine.executeBaselineRun("RUN-BASE-01", scenario);

        assertEquals("RUN-BASE-01", result.getRunId());
        assertEquals("SCENARIO-KT", result.getScenarioId());

        SimulationRun run = result.getSimulationRun();
        assertEquals(SimulationRun.Status.COMPLETED, run.getStatus());
        assertEquals(720, run.getCurrentTick());
        assertEquals(3600.0, run.getElapsedTimeSeconds());

        // Verify tracked encounters
        List<Encounter> encounters = result.getEncounters();
        assertFalse(encounters.isEmpty(), "Expected encounters in known-threat baseline run.");

        for (Encounter e : encounters) {
            assertEquals("RUN-BASE-01", e.getRunId());
            assertNotNull(e.getRiskScore());
        }

        // Verify threat ranking via CustomMaxHeap
        List<Encounter> ranked = result.getRankedThreats();
        assertEquals(encounters.size(), ranked.size());
        for (int i = 0; i < ranked.size() - 1; i++) {
            assertTrue(ranked.get(i).getRiskScore().getTotalScore()
                    >= ranked.get(i + 1).getRiskScore().getTotalScore());
        }

        // Verify graph construction
        OrbitalGraph graph = result.getEncounterGraph();
        assertNotNull(graph);
        List<List<String>> clusters = graph.findConnectedRiskClusters();
        assertFalse(clusters.isEmpty());
    }

    @Test
    void plansManeuversAndReplaysViaFacade() {
        OrbitalEngine engine = new OrbitalEngine();
        Scenario scenario = engine.generateKnownThreatScenario("SCENARIO-KT", 42L);

        // 1. Execute baseline run to completion (720 ticks / 3600 seconds)
        SimulationExecutionResult baseResult = engine.executeBaselineRun("RUN-BASE-02", scenario);
        SimulationRun baseRun = baseResult.getSimulationRun();
        assertEquals(SimulationRun.Status.COMPLETED, baseRun.getStatus());
        assertEquals(720, baseRun.getCurrentTick());

        // 2. Authentic encounter naturally reached RESOLVED status in completed baseline
        Encounter topThreat = baseResult.getRankedThreats().get(0);
        assertEquals(Encounter.Status.RESOLVED, topThreat.getStatus(),
                "Authentic baseline encounter must naturally reach RESOLVED status upon separation.");

        // Snapshot baseline satellite and debris final states
        SpaceObject baseSat = baseRun.getObjects().get(0);
        SpaceObject baseDeb = baseRun.getObjects().get(1);
        Vector3D baseSatFinalPos = baseSat.getPosition();
        Vector3D baseSatVel = baseSat.getVelocity();
        Vector3D baseDebFinalPos = baseDeb.getPosition();
        Vector3D baseDebVel = baseDeb.getVelocity();

        // 3. Create immutable ManeuverReplayInput directly from baseline result and authentic encounter
        ManeuverReplayInput replayInput = engine.createReplayInput(baseResult, topThreat, baseSat.getId());
        assertEquals("RUN-BASE-02", replayInput.getBaselineRunId());
        assertEquals(topThreat.getEncounterId(), replayInput.getEncounterId());
        assertEquals(topThreat.getDetectedAtTick(), replayInput.getDetectedAtTick());
        assertEquals(baseSat.getId(), replayInput.getTargetObjectId());
        assertEquals(baseDeb.getId(), replayInput.getOtherObjectId());

        // 4. Plan maneuvers using the detection-time state reconstructed from replay input
        List<ManeuverCandidate> candidates = engine.planManeuvers(replayInput);
        assertEquals(3, candidates.size());
        ManeuverCandidate chosen = candidates.get(0);
        assertEquals(topThreat.getEncounterId(), chosen.getEncounterId());
        assertEquals(baseSat.getId(), chosen.getTargetObjectId());

        // 5. Execute retrospective replay via facade
        ManeuverReplayResult replayResult = engine.executeManeuverReplay(
                "RUN-MANEUVER-02",
                replayInput,
                chosen
        );

        // 6. Verify replay IDs and linkage
        assertEquals("RUN-MANEUVER-02", replayResult.getRunId());
        assertEquals("RUN-BASE-02", replayResult.getBaselineRunId());
        assertEquals(chosen.getManeuverCandidateId(), replayResult.getManeuverCandidateId());
        assertEquals(SimulationRun.Status.COMPLETED, replayResult.getManeuverRun().getStatus());
        assertEquals(720, replayResult.getManeuverRun().getCurrentTick());
        assertEquals(3600.0, replayResult.getManeuverRun().getElapsedTimeSeconds());

        // 7. Verify baseline encounter and baseline run remain completely unchanged
        assertEquals(Encounter.Status.RESOLVED, topThreat.getStatus(),
                "Baseline encounter must remain in RESOLVED status and not be mutated in-place.");
        assertEquals(baseSatFinalPos, baseSat.getPosition());
        assertEquals(baseSatVel, baseSat.getVelocity());
        assertEquals(baseDebFinalPos, baseDeb.getPosition());
        assertEquals(baseDebVel, baseDeb.getVelocity());

        // 8. Verify maneuver run kinematics reflect the applied avoidance delta-v
        SpaceObject maneuveredSat = replayResult.getManeuverRun().getObjects().get(0);
        SpaceObject maneuveredDeb = replayResult.getManeuverRun().getObjects().get(1);

        // Target velocity includes delta-v; debris velocity remains baseline
        assertEquals(baseSatVel.add(chosen.getDeltaV()), maneuveredSat.getVelocity());
        assertEquals(baseDebVel, maneuveredDeb.getVelocity());

        // Verify displacement oracle: deltaV * (3600 - 5 * detectedAtTick)
        double remainingSeconds = SimulationConstants.SIMULATION_HORIZON_SECONDS
                - (replayInput.getDetectedAtTick() * SimulationConstants.DELTA_TIME_SECONDS);
        Vector3D expectedDisplacement = chosen.getDeltaV().scale(remainingSeconds);
        Vector3D expectedFinalPos = baseSatFinalPos.add(expectedDisplacement);

        assertEquals(expectedFinalPos.getX(), maneuveredSat.getPosition().getX(), 0.000001);
        assertEquals(expectedFinalPos.getY(), maneuveredSat.getPosition().getY(), 0.000001);
        assertEquals(expectedFinalPos.getZ(), maneuveredSat.getPosition().getZ(), 0.000001);

        // 9. Verify replay result exposes separate replay outcome metadata and recalculated encounters
        assertEquals(topThreat.getDetectedAtTick(), replayResult.getAppliedAtTick());
        assertEquals(chosen.getDeltaV(), replayResult.getAppliedDeltaV());
        assertEquals(baseSat.getId(), replayResult.getTargetObjectId());
        assertNotNull(replayResult.getRecalculatedEncounters());
    }

    @Test
    void verifiesManeuverDisplacementOracleAtNonZeroDetectionTick() {
        OrbitalEngine engine = new OrbitalEngine();
        Scenario scenario = engine.generateKnownThreatScenario("SCENARIO-KT", 42L);

        // Construct replay input with non-zero detectedAtTick = 10 (t = 50s)
        ManeuverReplayInput replayInput = new ManeuverReplayInput(
                "RUN-BASE-T10",
                scenario,
                "ENC-T10",
                10,
                "SAT-1",
                "DEB-1",
                10.0,
                100.0,
                2.0,
                new RiskScore(90, 0.9, 0.13, 0.95, RiskLabel.CRITICAL, "High threat")
        );

        Vector3D deltaV = new Vector3D(0.0, 0.020, 0.010); // in Y and Z
        ManeuverCandidate candidate = new ManeuverCandidate(
                "MAN-T10", "ENC-T10", "SAT-1", deltaV, 25.0, deltaV.magnitude(), 1);

        ManeuverReplayResult result = engine.executeManeuverReplay(
                "RUN-MAN-T10", replayInput, candidate);

        SimulationRun maneuverRun = result.getManeuverRun();
        assertEquals(SimulationRun.Status.COMPLETED, maneuverRun.getStatus());

        SpaceObject sat = maneuverRun.getObjects().get(0);

        // Under constant-velocity propagation:
        // Before tick 10 (first 50 seconds), velocity was initial (1.0, 0, 0).
        // At tick 10, deltaV (0, 0.020, 0.010) was added: velocity becomes (1.0, 0.020, 0.010).
        // Remaining time = 3600 - (10 * 5) = 3550 seconds.
        // Displacement relative to baseline (which had vy=0, vz=0 for all 3600s):
        // Delta Y = 0.020 * 3550 = 71.0 km
        // Delta Z = 0.010 * 3550 = 35.5 km
        assertEquals(71.0, sat.getPosition().getY(), 0.000001);
        assertEquals(35.5, sat.getPosition().getZ(), 0.000001);

        // Target X position: initial -100 + 1.0 * 3600 = 3500 km
        assertEquals(3500.0, sat.getPosition().getX(), 0.000001);
    }

    @Test
    void verifiesBaselineImmutabilityAcrossRetrospectiveReplay() {
        OrbitalEngine engine = new OrbitalEngine();
        Scenario scenario = engine.generateKnownThreatScenario("SCENARIO-KT", 42L);
        SimulationExecutionResult baseResult = engine.executeBaselineRun("RUN-BASE-IMMUT", scenario);

        SimulationRun baseRun = baseResult.getSimulationRun();
        Encounter baselineThreat = baseResult.getRankedThreats().get(0);

        // 1. Snapshot all baseline attributes before replay
        SimulationRun.Status preRunStatus = baseRun.getStatus();
        int preTick = baseRun.getCurrentTick();
        double preElapsed = baseRun.getElapsedTimeSeconds();
        List<Vector3D> prePositions = baseRun.getObjects().stream().map(SpaceObject::getPosition).toList();
        List<Vector3D> preVelocities = baseRun.getObjects().stream().map(SpaceObject::getVelocity).toList();
        List<SpaceObject.Status> preStatuses = baseRun.getObjects().stream().map(SpaceObject::getStatus).toList();

        String preEncId = baselineThreat.getEncounterId();
        Encounter.Status preEncStatus = baselineThreat.getStatus();
        double preMinSep = baselineThreat.getMinSeparationKm();
        double preTca = baselineThreat.getTimeToClosestApproachSeconds();
        double preRelVel = baselineThreat.getRelativeVelocityKmPerSec();
        int preEncScore = baselineThreat.getRiskScore().getTotalScore();

        // 2. Perform retrospective replay
        ManeuverReplayInput replayInput = engine.createReplayInput(baseResult, baselineThreat, "SAT-1");
        List<ManeuverCandidate> candidates = engine.planManeuvers(replayInput);
        ManeuverReplayResult replayResult = engine.executeManeuverReplay(
                "RUN-MAN-IMMUT", replayInput, candidates.get(0));

        // 3. Assert baseline run attributes are 100% untouched
        assertEquals(preRunStatus, baseRun.getStatus());
        assertEquals(preTick, baseRun.getCurrentTick());
        assertEquals(preElapsed, baseRun.getElapsedTimeSeconds());
        for (int i = 0; i < baseRun.getObjects().size(); i++) {
            assertEquals(prePositions.get(i), baseRun.getObjects().get(i).getPosition());
            assertEquals(preVelocities.get(i), baseRun.getObjects().get(i).getVelocity());
            assertEquals(preStatuses.get(i), baseRun.getObjects().get(i).getStatus());
        }

        // 4. Assert baseline encounter is 100% untouched
        assertEquals(preEncId, baselineThreat.getEncounterId());
        assertEquals(preEncStatus, baselineThreat.getStatus(), "Baseline encounter status must remain untouched.");
        assertEquals(preMinSep, baselineThreat.getMinSeparationKm(), 0.000001);
        assertEquals(preTca, baselineThreat.getTimeToClosestApproachSeconds(), 0.000001);
        assertEquals(preRelVel, baselineThreat.getRelativeVelocityKmPerSec(), 0.000001);
        assertEquals(preEncScore, baselineThreat.getRiskScore().getTotalScore());

        // 5. Assert replay result exposes separate replay outcome linked to baseline run and candidate
        assertEquals("RUN-BASE-IMMUT", replayResult.getBaselineRunId());
        assertEquals(candidates.get(0).getManeuverCandidateId(), replayResult.getManeuverCandidateId());
        assertEquals("RUN-MAN-IMMUT", replayResult.getRunId());
        assertNotNull(replayResult.getRecalculatedEncounters());
    }

    @Test
    void rejectsInvalidManeuverReplayInputAssociationsInFacade() {
        OrbitalEngine engine = new OrbitalEngine();
        Scenario scenario = engine.generateKnownThreatScenario("SCENARIO-KT", 42L);
        SimulationExecutionResult baseResult = engine.executeBaselineRun("RUN-BASE-VAL", scenario);

        Encounter threat = baseResult.getRankedThreats().get(0);

        // 1. Target object not in encounter pair
        assertThrows(IllegalArgumentException.class, () ->
                engine.createReplayInput(baseResult, threat, "UNKNOWN-OBJ"));

        // 2. Blank target object ID
        assertThrows(IllegalArgumentException.class, () ->
                engine.createReplayInput(baseResult, threat, ""));

        // 3. Encounter does not belong to baseline run
        Encounter alienEncounter = new Encounter(
                "ENC-ALIEN", "RUN-OTHER", "SAT-1", "DEB-1", 10.0, 100.0, 1.0, 0);
        assertThrows(IllegalArgumentException.class, () ->
                engine.createReplayInput(baseResult, alienEncounter, "SAT-1"));

        // 4. Failed baseline run rejected
        SimulationRun failedRun = new SimulationRun("RUN-FAIL", scenario, null, null);
        failedRun.start();
        failedRun.fail();
        assertThrows(IllegalArgumentException.class, () ->
                engine.createReplayInput(failedRun, threat, "SAT-1"));

        // 5. Replay candidate belonging to wrong encounter
        ManeuverReplayInput validInput = engine.createReplayInput(baseResult, threat, "SAT-1");
        Vector3D dv = new Vector3D(0, 0, 0.01);
        ManeuverCandidate wrongEncCandidate = new ManeuverCandidate(
                "MAN-WRONG", "ENC-ALIEN", "SAT-1", dv, 20.0, 0.01, 1);

        assertThrows(IllegalArgumentException.class, () ->
                engine.executeManeuverReplay("RUN-MAN-VAL", validInput, wrongEncCandidate));

        // 6. Replay candidate targeting wrong object
        ManeuverCandidate wrongTargetCandidate = new ManeuverCandidate(
                "MAN-WRONG-TARGET", threat.getEncounterId(), "DEB-1", dv, 20.0, 0.01, 1);

        assertThrows(IllegalArgumentException.class, () ->
                engine.executeManeuverReplay("RUN-MAN-VAL", validInput, wrongTargetCandidate));

        // 7. Null replay input
        assertThrows(NullPointerException.class, () ->
                engine.executeManeuverReplay("RUN-MAN-VAL", (ManeuverReplayInput) null, wrongEncCandidate));
    }

    @Test
    void replayProducesDeterministicResultsAcrossRepeatedExecutions() {
        OrbitalEngine engine = new OrbitalEngine();
        Scenario scenario = engine.generateKnownThreatScenario("SCENARIO-KT", 42L);
        SimulationExecutionResult baseResult = engine.executeBaselineRun("RUN-BASE-DET", scenario);

        Encounter threat = baseResult.getRankedThreats().get(0);
        ManeuverReplayInput replayInput = engine.createReplayInput(baseResult, threat, "SAT-1");

        List<ManeuverCandidate> candidates = engine.planManeuvers(replayInput);
        ManeuverCandidate chosen = candidates.get(0);

        ManeuverReplayResult res1 = engine.executeManeuverReplay("RUN-MAN-1", replayInput, chosen);
        ManeuverReplayResult res2 = engine.executeManeuverReplay("RUN-MAN-2", replayInput, chosen);

        // Final positions and velocities must match exactly across repeated runs
        SpaceObject sat1 = res1.getManeuverRun().getObjects().get(0);
        SpaceObject sat2 = res2.getManeuverRun().getObjects().get(0);
        assertEquals(sat1.getPosition(), sat2.getPosition());
        assertEquals(sat1.getVelocity(), sat2.getVelocity());

        SpaceObject deb1 = res1.getManeuverRun().getObjects().get(1);
        SpaceObject deb2 = res2.getManeuverRun().getObjects().get(1);
        assertEquals(deb1.getPosition(), deb2.getPosition());
        assertEquals(deb1.getVelocity(), deb2.getVelocity());

        // Recalculated encounter counts must match
        assertEquals(res1.getRecalculatedEncounters().size(), res2.getRecalculatedEncounters().size());
    }

    @Test
    void generatesScenariosAndRunsBenchmarksViaFacade() {
        OrbitalEngine engine = new OrbitalEngine();

        // Scenario generation
        ScenarioGenerationConfig config = new ScenarioGenerationConfig(
                5, 5, -1000, 1000, -1, 1, 0.01, 0.1);
        Scenario scenario = engine.generateScenario("SCENARIO-GEN", config, 99L);
        assertEquals("SCENARIO-GEN", scenario.getScenarioId());
        assertEquals(10, scenario.getInitialObjectStates().size());

        // Benchmark execution
        SpatialGridBenchmark.BenchmarkResult benchResult = engine.runBenchmark(50, 42L);
        assertEquals(50, benchResult.getObjectCount());
        assertTrue(benchResult.isCorrectnessVerified());
        assertTrue(benchResult.getPairReductionPercentage() > 80.0);

        List<SpatialGridBenchmark.BenchmarkResult> allBench =
                engine.runAllBenchmarks(new int[]{50, 100}, 42L);
        assertEquals(2, allBench.size());
    }

    @Test
    void rejectsInvalidInputsAndSurfacesErrorsConsistently() {
        OrbitalEngine engine = new OrbitalEngine();

        assertThrows(IllegalArgumentException.class, () ->
                engine.executeBaselineRun("", engine.generateKnownThreatScenario("S1", 1L)));
        assertThrows(IllegalArgumentException.class, () ->
                engine.executeBaselineRun(null, engine.generateKnownThreatScenario("S1", 1L)));
        assertThrows(NullPointerException.class, () ->
                engine.executeBaselineRun("RUN-1", null));

        // Result model validation
        SimulationRun dummyRun = new SimulationRun("R1",
                engine.generateKnownThreatScenario("S1", 1L), null, null);
        assertThrows(IllegalArgumentException.class, () ->
                new SimulationExecutionResult("", "S1", dummyRun, null, null, null));
        assertThrows(IllegalArgumentException.class, () ->
                new SimulationExecutionResult("R1", "", dummyRun, null, null, null));
        assertThrows(NullPointerException.class, () ->
                new SimulationExecutionResult("R1", "S1", null, null, null, null));
    }

    @Test
    void marksRunFailedAndRethrowsWhenRuntimeExceptionOccursDuringTickProcessing() {
        OrbitalEngine engine = new OrbitalEngine();

        // Construct scenario with two objects having the same ID within close proximity (<= 100km).
        // During tick processing, EncounterDetectionService detects a close approach between them,
        // but EncounterTracker rejects duplicate object IDs in an encounter pair with IllegalArgumentException.
        InitialObjectState obj1 = new InitialObjectState(
                "DUPLICATE-ID",
                SpaceObject.ObjectType.DEBRIS,
                new Vector3D(0.0, 0.0, 0.0),
                new Vector3D(0.0, 0.0, 0.0),
                1.0,
                SpaceObject.Status.ACTIVE,
                null,
                null
        );
        InitialObjectState obj2 = new InitialObjectState(
                "DUPLICATE-ID",
                SpaceObject.ObjectType.DEBRIS,
                new Vector3D(5.0, 0.0, 0.0),
                new Vector3D(0.0, 0.0, 0.0),
                1.0,
                SpaceObject.Status.ACTIVE,
                null,
                null
        );

        ScenarioGenerationConfig config = new ScenarioGenerationConfig(
                0, 2, -100, 100, -1, 1, 0.1, 1.0);
        Scenario failingScenario = new Scenario(
                "SCENARIO-FAIL",
                config,
                123L,
                List.of(obj1, obj2),
                Scenario.Status.READY
        );

        // 1. Verify that the original RuntimeException (IllegalArgumentException) is rethrown and not swallowed
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                engine.executeBaselineRun("RUN-FAIL-01", failingScenario));

        assertTrue(ex.getMessage().contains("different objects"));

        // 2. Verify that the simulation run failure lifecycle correctly transitions from RUNNING to FAILED
        SimulationRun standaloneRun = new SimulationRun(
                "RUN-FAIL-01", failingScenario, null, null);
        standaloneRun.start();
        assertEquals(SimulationRun.Status.RUNNING, standaloneRun.getStatus());
        standaloneRun.fail();
        assertEquals(SimulationRun.Status.FAILED, standaloneRun.getStatus());
        assertThrows(IllegalStateException.class, standaloneRun::advanceTick);
    }

    @Test
    void executesNoThreatScenarioProducingZeroEncountersAndEmptyRankings() {
        OrbitalEngine engine = new OrbitalEngine();

        // Construct well-separated objects moving away from each other:
        // SAT-1 at +10,000 km moving in +X direction (+0.5 km/s)
        // DEB-1 at -10,000 km moving in -X direction (-0.5 km/s)
        // SAT-2 at +10,000 km on Y moving in +Y direction (+0.5 km/s)
        // Initial separation between every pair is >= 14,142 km, strictly diverging over all 3,600 s.
        // Even at closest approach, minimum separation remains >= 14,142 km >> 100 km threshold.
        InitialObjectState sat1 = new InitialObjectState(
                "SAT-1",
                SpaceObject.ObjectType.SATELLITE,
                new Vector3D(10000.0, 0.0, 0.0),
                new Vector3D(0.5, 0.0, 0.0),
                1.0,
                SpaceObject.Status.ACTIVE,
                "Satellite-1",
                null
        );
        InitialObjectState deb1 = new InitialObjectState(
                "DEB-1",
                SpaceObject.ObjectType.DEBRIS,
                new Vector3D(-10000.0, 0.0, 0.0),
                new Vector3D(-0.5, 0.0, 0.0),
                1.0,
                SpaceObject.Status.ACTIVE,
                null,
                null
        );
        InitialObjectState sat2 = new InitialObjectState(
                "SAT-2",
                SpaceObject.ObjectType.SATELLITE,
                new Vector3D(0.0, 10000.0, 0.0),
                new Vector3D(0.0, 0.5, 0.0),
                1.0,
                SpaceObject.Status.ACTIVE,
                "Satellite-2",
                null
        );

        ScenarioGenerationConfig config = new ScenarioGenerationConfig(
                2, 1, -15000, 15000, -1, 1, 0.1, 2.0);
        Scenario noThreatScenario = new Scenario(
                "SCENARIO-NO-THREAT",
                config,
                999L,
                List.of(sat1, deb1, sat2),
                Scenario.Status.READY
        );

        SimulationExecutionResult result = engine.executeBaselineRun("RUN-NO-THREAT", noThreatScenario);

        assertEquals("RUN-NO-THREAT", result.getRunId());
        assertEquals("SCENARIO-NO-THREAT", result.getScenarioId());

        // Simulation completes full 3,600s horizon
        SimulationRun run = result.getSimulationRun();
        assertEquals(SimulationRun.Status.COMPLETED, run.getStatus());
        assertEquals(720, run.getCurrentTick());
        assertEquals(3600.0, run.getElapsedTimeSeconds());

        // Verify objects propagated to expected separated positions at t=3600s
        // sat1: 10000 + 0.5 * 3600 = 11800 km on X
        // deb1: -10000 - 0.5 * 3600 = -11800 km on X
        // sat2: 10000 + 0.5 * 3600 = 11800 km on Y
        List<SpaceObject> objects = run.getObjects();
        assertEquals(3, objects.size());
        assertEquals(new Vector3D(11800.0, 0.0, 0.0), objects.get(0).getPosition());
        assertEquals(new Vector3D(-11800.0, 0.0, 0.0), objects.get(1).getPosition());
        assertEquals(new Vector3D(0.0, 11800.0, 0.0), objects.get(2).getPosition());

        // No qualifying encounters or threats throughout the entire horizon
        assertTrue(result.getEncounters().isEmpty(), "Expected 0 encounters for well-separated scenario.");
        assertTrue(result.getRankedThreats().isEmpty(), "Expected 0 ranked threats for well-separated scenario.");

        // Encounter graph has 0 vertices and 0 edges
        OrbitalGraph graph = result.getEncounterGraph();
        assertNotNull(graph);
        assertEquals(0, graph.vertexCount());
        assertEquals(0, graph.edgeCount());
        assertTrue(graph.findConnectedRiskClusters().isEmpty());
    }

    @Test
    void executesBaselineRunDeterministicallyAcrossRepeatedInvocations() {
        OrbitalEngine engine = new OrbitalEngine();

        // Generate scenario twice with identical seed and config
        Scenario scenarioA = engine.generateKnownThreatScenario("SCENARIO-KT", 42L);
        Scenario scenarioB = engine.generateKnownThreatScenario("SCENARIO-KT", 42L);

        // 1. Initial object states match exactly
        assertEquals(scenarioA.getInitialObjectStates().size(), scenarioB.getInitialObjectStates().size());
        for (int i = 0; i < scenarioA.getInitialObjectStates().size(); i++) {
            InitialObjectState stateA = scenarioA.getInitialObjectStates().get(i);
            InitialObjectState stateB = scenarioB.getInitialObjectStates().get(i);
            assertEquals(stateA.getId(), stateB.getId());
            assertEquals(stateA.getType(), stateB.getType());
            assertEquals(stateA.getPosition(), stateB.getPosition());
            assertEquals(stateA.getVelocity(), stateB.getVelocity());
            assertEquals(stateA.getSize(), stateB.getSize());
            assertEquals(stateA.getStatus(), stateB.getStatus());
        }

        // 2. Execute two separate baseline runs
        SimulationExecutionResult resultA = engine.executeBaselineRun("RUN-BASE-A", scenarioA);
        SimulationExecutionResult resultB = engine.executeBaselineRun("RUN-BASE-B", scenarioB);

        // 3. Compare simulation progress and final status
        SimulationRun runA = resultA.getSimulationRun();
        SimulationRun runB = resultB.getSimulationRun();
        assertEquals(SimulationRun.Status.COMPLETED, runA.getStatus());
        assertEquals(runA.getStatus(), runB.getStatus());
        assertEquals(runA.getCurrentTick(), runB.getCurrentTick());
        assertEquals(runA.getElapsedTimeSeconds(), runB.getElapsedTimeSeconds());

        // 4. Compare final object states
        List<SpaceObject> objectsA = runA.getObjects();
        List<SpaceObject> objectsB = runB.getObjects();
        assertEquals(objectsA.size(), objectsB.size());
        for (int i = 0; i < objectsA.size(); i++) {
            SpaceObject objA = objectsA.get(i);
            SpaceObject objB = objectsB.get(i);
            assertNotSame(objA, objB); // Independent instances
            assertEquals(objA.getId(), objB.getId());
            assertEquals(objA.getType(), objB.getType());
            assertEquals(objA.getPosition(), objB.getPosition());
            assertEquals(objA.getVelocity(), objB.getVelocity());
            assertEquals(objA.getStatus(), objB.getStatus());
        }

        // 5. Compare encounter outcomes (canonical pair, detection tick, measurements, status, risk score)
        List<Encounter> encountersA = resultA.getEncounters();
        List<Encounter> encountersB = resultB.getEncounters();
        assertEquals(encountersA.size(), encountersB.size());
        assertFalse(encountersA.isEmpty());
        for (int i = 0; i < encountersA.size(); i++) {
            Encounter encA = encountersA.get(i);
            Encounter encB = encountersB.get(i);
            assertEquals(encA.getObjectAId(), encB.getObjectAId());
            assertEquals(encA.getObjectBId(), encB.getObjectBId());
            assertEquals(encA.getDetectedAtTick(), encB.getDetectedAtTick());
            assertEquals(encA.getMinSeparationKm(), encB.getMinSeparationKm(), 0.000001);
            assertEquals(encA.getTimeToClosestApproachSeconds(), encB.getTimeToClosestApproachSeconds(), 0.000001);
            assertEquals(encA.getRelativeVelocityKmPerSec(), encB.getRelativeVelocityKmPerSec(), 0.000001);
            assertEquals(encA.getStatus(), encB.getStatus());

            RiskScore scoreA = encA.getRiskScore();
            RiskScore scoreB = encB.getRiskScore();
            assertNotNull(scoreA);
            assertNotNull(scoreB);
            assertEquals(scoreA.getTotalScore(), scoreB.getTotalScore());
            assertEquals(scoreA.getLabel(), scoreB.getLabel());
            assertEquals(scoreA.getSeparationFactor(), scoreB.getSeparationFactor(), 0.000001);
            assertEquals(scoreA.getVelocityFactor(), scoreB.getVelocityFactor(), 0.000001);
            assertEquals(scoreA.getTimeFactor(), scoreB.getTimeFactor(), 0.000001);
            assertEquals(scoreA.getExplanation(), scoreB.getExplanation());
        }

        // 6. Compare threat ranking order and scores
        List<Encounter> rankedA = resultA.getRankedThreats();
        List<Encounter> rankedB = resultB.getRankedThreats();
        assertEquals(rankedA.size(), rankedB.size());
        for (int i = 0; i < rankedA.size(); i++) {
            Encounter threatA = rankedA.get(i);
            Encounter threatB = rankedB.get(i);
            assertEquals(threatA.getObjectAId(), threatB.getObjectAId());
            assertEquals(threatA.getObjectBId(), threatB.getObjectBId());
            assertEquals(threatA.getRiskScore().getTotalScore(), threatB.getRiskScore().getTotalScore());
            assertEquals(threatA.getRiskScore().getLabel(), threatB.getRiskScore().getLabel());
        }

        // 7. Compare encounter graph structure
        OrbitalGraph graphA = resultA.getEncounterGraph();
        OrbitalGraph graphB = resultB.getEncounterGraph();
        assertEquals(graphA.vertexCount(), graphB.vertexCount());
        assertEquals(graphA.edgeCount(), graphB.edgeCount());
        assertEquals(graphA.getVertices(), graphB.getVertices());
    }
}
