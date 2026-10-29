package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ManeuverReplayServiceTest {

    private Scenario createKnownThreatScenario() {
        ScenarioGenerator generator = new ScenarioGenerator();
        return generator.generateKnownThreatScenario("SCENARIO-THREAT-1", 42L);
    }

    private SimulationRun createBaselineRun(String runId, Scenario scenario) {
        return new SimulationRun(runId, scenario, null, null);
    }

    private Encounter createAssessedEncounter(
            String encounterId,
            String runId,
            String satId,
            String debId,
            int detectedAtTick) {

        Encounter enc = new Encounter(
                encounterId,
                runId,
                satId,
                debId,
                0.0,
                100.0,
                2.0,
                detectedAtTick
        );
        RiskScore score = new RiskScore(
                95, 1.0, 0.13, 0.97,
                RiskLabel.CRITICAL, "Critical collision risk"
        );
        enc.assess(score);
        return enc;
    }

    @Test
    void successfullyReplaysManeuverInSeparateRunAndPreservesBaseline() {
        Scenario scenario = createKnownThreatScenario();
        SimulationRun baselineRun = createBaselineRun("RUN-BASE-01", scenario);
        baselineRun.start();
        while (baselineRun.getStatus() == SimulationRun.Status.RUNNING) {
            baselineRun.advanceTick();
        }
        assertEquals(SimulationRun.Status.COMPLETED, baselineRun.getStatus());

        // Snapshot baseline state at completion
        SpaceObject baseSat = baselineRun.getObjects().get(0);
        SpaceObject baseDeb = baselineRun.getObjects().get(1);
        Vector3D baseSatFinalPos = baseSat.getPosition();
        Vector3D baseSatVel = baseSat.getVelocity();
        Vector3D baseDebFinalPos = baseDeb.getPosition();
        Vector3D baseDebVel = baseDeb.getVelocity();

        Encounter baselineEncounter = createAssessedEncounter(
                "ENC-01", baselineRun.getRunId(), "SAT-1", "DEB-1", 10);
        assertEquals(Encounter.Status.ASSESSED, baselineEncounter.getStatus());

        Vector3D deltaV = new Vector3D(0.0, 0.0, 0.010); // Out-of-plane impulse
        ManeuverCandidate candidate = new ManeuverCandidate(
                "MAN-01", baselineEncounter.getEncounterId(), "SAT-1",
                deltaV, 15.0, deltaV.magnitude(), 1
        );

        ManeuverReplayService replayService = new ManeuverReplayService();
        ManeuverReplayResult result = replayService.replayAndApply(
                "RUN-MANEUVER-01",
                baselineRun,
                baselineEncounter,
                candidate
        );

        // 1. Verify IDs and metadata
        assertEquals("RUN-MANEUVER-01", result.getRunId());
        assertEquals("RUN-BASE-01", result.getBaselineRunId());
        assertEquals("MAN-01", result.getManeuverCandidateId());
        assertEquals(10, result.getAppliedAtTick());
        assertEquals(deltaV, result.getAppliedDeltaV());
        assertEquals("SAT-1", result.getTargetObjectId());

        // 2. Baseline run and objects remain completely unchanged
        assertEquals(baseSatFinalPos, baseSat.getPosition());
        assertEquals(baseSatVel, baseSat.getVelocity());
        assertEquals(baseDebFinalPos, baseDeb.getPosition());
        assertEquals(baseDebVel, baseDeb.getVelocity());

        // 3. Baseline encounter remains unchanged
        assertEquals(Encounter.Status.ASSESSED, baselineEncounter.getStatus(),
                "Baseline encounter must not be mutated in-place.");

        // 4. Maneuver run is completed over full horizon
        SimulationRun maneuverRun = result.getManeuverRun();
        assertEquals("RUN-MANEUVER-01", maneuverRun.getRunId());
        assertEquals("RUN-BASE-01", maneuverRun.getBaselineRunId());
        assertEquals("MAN-01", maneuverRun.getManeuverCandidateId());
        assertEquals(SimulationRun.Status.COMPLETED, maneuverRun.getStatus());
        assertEquals(720, maneuverRun.getCurrentTick());
        assertEquals(3600.0, maneuverRun.getElapsedTimeSeconds());

        // 5. Delta-v applied to target only
        SpaceObject manSat = maneuverRun.getObjects().get(0);
        SpaceObject manDeb = maneuverRun.getObjects().get(1);

        assertEquals(new Vector3D(1.0, 0.0, 0.010), manSat.getVelocity());
        assertEquals(new Vector3D(-1.0, 0.0, 0.0), manDeb.getVelocity()); // Deb unchanged

        // SAT-1 moved in Z axis after tick 10 (3550 seconds of motion with vz = 0.010)
        double expectedZ = 0.010 * (3600.0 - (10 * 5.0));
        assertEquals(expectedZ, manSat.getPosition().getZ(), 0.000001);
        assertEquals(0.0, manDeb.getPosition().getZ(), 0.000001);

        // 6. Recalculated encounters exist
        assertNotNull(result.getRecalculatedEncounters());
    }

    @Test
    void successfullyReplaysRetrospectiveManeuverUsingManeuverReplayInput() {
        OrbitalEngine engine = new OrbitalEngine();
        Scenario scenario = createKnownThreatScenario();
        SimulationExecutionResult baseResult = engine.executeBaselineRun("RUN-BASE-INPUT", scenario);

        SimulationRun baseRun = baseResult.getSimulationRun();
        assertEquals(SimulationRun.Status.COMPLETED, baseRun.getStatus());

        Encounter resolvedEncounter = baseResult.getRankedThreats().get(0);
        assertEquals(Encounter.Status.RESOLVED, resolvedEncounter.getStatus(),
                "Authentic baseline encounter must naturally reach RESOLVED status.");

        ManeuverReplayInput replayInput = engine.createReplayInput(
                baseResult, resolvedEncounter, "SAT-1");

        Vector3D deltaV = new Vector3D(0.0, 0.010, 0.0);
        ManeuverCandidate candidate = new ManeuverCandidate(
                "MAN-INPUT-01", resolvedEncounter.getEncounterId(), "SAT-1",
                deltaV, 20.0, deltaV.magnitude(), 1);

        ManeuverReplayService service = new ManeuverReplayService();
        ManeuverReplayResult result = service.replayAndApply(
                "RUN-MAN-INPUT", replayInput, candidate);

        assertEquals("RUN-MAN-INPUT", result.getRunId());
        assertEquals("RUN-BASE-INPUT", result.getBaselineRunId());
        assertEquals("MAN-INPUT-01", result.getManeuverCandidateId());
        assertEquals(resolvedEncounter.getDetectedAtTick(), result.getAppliedAtTick());
        assertEquals(SimulationRun.Status.COMPLETED, result.getManeuverRun().getStatus());

        // Baseline encounter remains RESOLVED
        assertEquals(Encounter.Status.RESOLVED, resolvedEncounter.getStatus());
        assertNotNull(result.getRecalculatedEncounters());
    }

    @Test
    void replayProducesDeterministicResultsForIdenticalInputs() {
        Scenario scenario = createKnownThreatScenario();
        SimulationRun baselineRun = createBaselineRun("RUN-BASE-02", scenario);
        baselineRun.start();

        Encounter enc1 = createAssessedEncounter("ENC-DETERM", baselineRun.getRunId(), "SAT-1", "DEB-1", 5);
        Encounter enc2 = createAssessedEncounter("ENC-DETERM", baselineRun.getRunId(), "SAT-1", "DEB-1", 5);

        Vector3D deltaV = new Vector3D(0.0, 0.020, 0.0);
        ManeuverCandidate cand1 = new ManeuverCandidate("MAN-01", enc1.getEncounterId(), "SAT-1", deltaV, 20.0, 0.020, 1);
        ManeuverCandidate cand2 = new ManeuverCandidate("MAN-01", enc2.getEncounterId(), "SAT-1", deltaV, 20.0, 0.020, 1);

        ManeuverReplayService service = new ManeuverReplayService();
        ManeuverReplayResult res1 = service.replayAndApply("RUN-MAN-A", baselineRun, enc1, cand1);
        ManeuverReplayResult res2 = service.replayAndApply("RUN-MAN-B", baselineRun, enc2, cand2);

        // Final positions must match exactly
        SpaceObject sat1 = res1.getManeuverRun().getObjects().get(0);
        SpaceObject sat2 = res2.getManeuverRun().getObjects().get(0);
        assertEquals(sat1.getPosition(), sat2.getPosition());
        assertEquals(sat1.getVelocity(), sat2.getVelocity());

        SpaceObject deb1 = res1.getManeuverRun().getObjects().get(1);
        SpaceObject deb2 = res2.getManeuverRun().getObjects().get(1);
        assertEquals(deb1.getPosition(), deb2.getPosition());
        assertEquals(deb1.getVelocity(), deb2.getVelocity());
    }

    @Test
    void rejectsInvalidAssociationsAndStates() {
        Scenario scenario = createKnownThreatScenario();
        SimulationRun baselineRun = createBaselineRun("RUN-BASE-03", scenario);
        baselineRun.start();

        Encounter validEnc = createAssessedEncounter("ENC-01", baselineRun.getRunId(), "SAT-1", "DEB-1", 10);
        Vector3D dv = new Vector3D(0, 0, 0.01);
        ManeuverCandidate validCand = new ManeuverCandidate("MAN-01", "ENC-01", "SAT-1", dv, 10.0, 0.01, 1);

        ManeuverReplayService service = new ManeuverReplayService();

        // 1. Blank or null newRunId
        assertThrows(IllegalArgumentException.class, () ->
                service.replayAndApply("", baselineRun, validEnc, validCand));
        assertThrows(IllegalArgumentException.class, () ->
                service.replayAndApply(null, baselineRun, validEnc, validCand));

        // 2. newRunId equals baselineRunId
        assertThrows(IllegalArgumentException.class, () ->
                service.replayAndApply(baselineRun.getRunId(), baselineRun, validEnc, validCand));

        // 3. Null components
        assertThrows(NullPointerException.class, () ->
                service.replayAndApply("NEW-RUN", (SimulationRun) null, validEnc, validCand));
        assertThrows(NullPointerException.class, () ->
                service.replayAndApply("NEW-RUN", baselineRun, null, validCand));
        assertThrows(NullPointerException.class, () ->
                service.replayAndApply("NEW-RUN", baselineRun, validEnc, null));

        // 4. Failed baseline run
        SimulationRun failedRun = createBaselineRun("RUN-FAILED", scenario);
        failedRun.fail();
        assertThrows(IllegalArgumentException.class, () ->
                service.replayAndApply("NEW-RUN", failedRun, validEnc, validCand));

        // 5. Candidate does not belong to encounter
        ManeuverCandidate alienCand = new ManeuverCandidate("MAN-ALIEN", "ENC-OTHER", "SAT-1", dv, 10.0, 0.01, 1);
        assertThrows(IllegalArgumentException.class, () ->
                service.replayAndApply("NEW-RUN", baselineRun, validEnc, alienCand));

        // 6. Encounter does not belong to baseline run
        Encounter alienEnc = createAssessedEncounter("ENC-01", "RUN-OTHER", "SAT-1", "DEB-1", 10);
        assertThrows(IllegalArgumentException.class, () ->
                service.replayAndApply("NEW-RUN", baselineRun, alienEnc, validCand));

        // 7. Unassessed encounter cannot be mitigated
        Encounter unassessedEnc = new Encounter(
                "ENC-RAW", baselineRun.getRunId(), "SAT-1", "DEB-1", 10.0, 50.0, 2.0, 10);
        ManeuverCandidate rawCand = new ManeuverCandidate("MAN-RAW", "ENC-RAW", "SAT-1", dv, 10.0, 0.01, 1);
        assertThrows(IllegalStateException.class, () ->
                service.replayAndApply("NEW-RUN", baselineRun, unassessedEnc, rawCand));

        // 8. Target object does not belong to encounter
        ManeuverCandidate outsiderTargetCand = new ManeuverCandidate("MAN-01", "ENC-01", "UNKNOWN-OBJ", dv, 10.0, 0.01, 1);
        assertThrows(IllegalArgumentException.class, () ->
                service.replayAndApply("NEW-RUN", baselineRun, validEnc, outsiderTargetCand));

        // 9. Encounter detection tick beyond horizon
        Encounter outOfBoundsTickEnc = createAssessedEncounter("ENC-01", baselineRun.getRunId(), "SAT-1", "DEB-1", 721);
        assertThrows(IllegalArgumentException.class, () ->
                service.replayAndApply("NEW-RUN", baselineRun, outOfBoundsTickEnc, validCand));
    }

    @Test
    void rejectsInvalidManeuverReplayInputAndCandidateCombinations() {
        Scenario scenario = createKnownThreatScenario();
        ManeuverReplayService service = new ManeuverReplayService();

        ManeuverReplayInput validInput = new ManeuverReplayInput(
                "RUN-BASE-01", scenario, "ENC-01", 10, "SAT-1", "DEB-1",
                10.0, 100.0, 2.0, null
        );
        Vector3D dv = new Vector3D(0, 0, 0.01);
        ManeuverCandidate validCand = new ManeuverCandidate("MAN-01", "ENC-01", "SAT-1", dv, 15.0, 0.01, 1);

        // Null inputs
        assertThrows(IllegalArgumentException.class, () ->
                service.replayAndApply("", validInput, validCand));
        assertThrows(NullPointerException.class, () ->
                service.replayAndApply("NEW-RUN", (ManeuverReplayInput) null, validCand));
        assertThrows(NullPointerException.class, () ->
                service.replayAndApply("NEW-RUN", validInput, null));

        // Run ID matches baseline run ID
        assertThrows(IllegalArgumentException.class, () ->
                service.replayAndApply("RUN-BASE-01", validInput, validCand));

        // Candidate encounter ID mismatch
        ManeuverCandidate wrongEncCand = new ManeuverCandidate("MAN-02", "ENC-MISMATCH", "SAT-1", dv, 15.0, 0.01, 1);
        assertThrows(IllegalArgumentException.class, () ->
                service.replayAndApply("NEW-RUN", validInput, wrongEncCand));

        // Candidate target object ID mismatch
        ManeuverCandidate wrongTargetCand = new ManeuverCandidate("MAN-03", "ENC-01", "DEB-1", dv, 15.0, 0.01, 1);
        assertThrows(IllegalArgumentException.class, () ->
                service.replayAndApply("NEW-RUN", validInput, wrongTargetCand));
    }

    @Test
    void appliesManeuverAtDetectedTickZeroBeforeFirstAdvance() {
        Scenario scenario = createKnownThreatScenario();
        SimulationRun baselineRun = createBaselineRun("RUN-BASE-T0", scenario);
        baselineRun.start();

        Encounter encT0 = createAssessedEncounter("ENC-T0", baselineRun.getRunId(), "SAT-1", "DEB-1", 0);
        Vector3D deltaV = new Vector3D(0.0, 0.050, 0.0);
        ManeuverCandidate cand = new ManeuverCandidate("MAN-T0", encT0.getEncounterId(), "SAT-1", deltaV, 20.0, 0.050, 1);

        ManeuverReplayService service = new ManeuverReplayService();
        ManeuverReplayResult result = service.replayAndApply("RUN-MAN-T0", baselineRun, encT0, cand);

        assertEquals(0, result.getAppliedAtTick());
        // Baseline encounter remains untouched
        assertEquals(Encounter.Status.ASSESSED, encT0.getStatus());

        SimulationRun manRun = result.getManeuverRun();
        assertEquals(SimulationRun.Status.COMPLETED, manRun.getStatus());

        SpaceObject sat = manRun.getObjects().get(0);
        SpaceObject deb = manRun.getObjects().get(1);

        // SAT-1 velocity received delta-v at tick 0: velocity is now (1.0, 0.050, 0.0)
        assertEquals(new Vector3D(1.0, 0.050, 0.0), sat.getVelocity());
        assertEquals(new Vector3D(-1.0, 0.0, 0.0), deb.getVelocity());

        // Target followed maneuvered velocity from the beginning:
        // After 720 ticks (3600 seconds), y = 0.050 * 3600 = 180.0
        assertEquals(180.0, sat.getPosition().getY(), 0.000001);
        assertEquals(3500.0, sat.getPosition().getX(), 0.000001);
        assertEquals(0.0, sat.getPosition().getZ(), 0.000001);

        // Debris followed baseline trajectory from the beginning
        assertEquals(0.0, deb.getPosition().getY(), 0.000001);
        assertEquals(-3500.0, deb.getPosition().getX(), 0.000001);

        // Baseline run remains untouched
        assertEquals(new Vector3D(1.0, 0.0, 0.0), baselineRun.getObjects().get(0).getVelocity());
    }

    @Test
    void failsManeuverRunAndPreservesBaselineWhenTargetIsInactiveAtManeuverTick() {
        InitialObjectState inactiveSat = new InitialObjectState(
                "SAT-1",
                SpaceObject.ObjectType.SATELLITE,
                new Vector3D(-100.0, 0.0, 0.0),
                new Vector3D(1.0, 0.0, 0.0),
                1.0,
                SpaceObject.Status.INACTIVE,
                "Test-Satellite-1",
                null
        );
        InitialObjectState activeDeb = new InitialObjectState(
                "DEB-1",
                SpaceObject.ObjectType.DEBRIS,
                new Vector3D(100.0, 0.0, 0.0),
                new Vector3D(-1.0, 0.0, 0.0),
                1.0,
                SpaceObject.Status.ACTIVE,
                null,
                null
        );
        Scenario scenario = new Scenario(
                "SCENARIO-INACTIVE",
                new ScenarioGenerationConfig(1, 1, -100, 100, -1, 1, 1.0, 1.0),
                42L,
                List.of(inactiveSat, activeDeb),
                Scenario.Status.READY
        );

        SimulationRun baselineRun = createBaselineRun("RUN-BASE-FAIL", scenario);
        baselineRun.start();

        Encounter baselineEnc = createAssessedEncounter("ENC-FAIL", baselineRun.getRunId(), "SAT-1", "DEB-1", 10);
        Vector3D dv = new Vector3D(0, 0, 0.010);
        ManeuverCandidate cand = new ManeuverCandidate("MAN-01", baselineEnc.getEncounterId(), "SAT-1", dv, 15.0, 0.010, 1);

        ManeuverReplayService service = new ManeuverReplayService();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.replayAndApply("RUN-MAN-FAIL", baselineRun, baselineEnc, cand));

        assertTrue(ex.getMessage().contains("inactive"));

        // Baseline encounter remains ASSESSED (not MITIGATED)
        assertEquals(Encounter.Status.ASSESSED, baselineEnc.getStatus());

        // Baseline run remains in its valid state
        assertEquals(SimulationRun.Status.RUNNING, baselineRun.getStatus());
        assertEquals("RUN-BASE-FAIL", baselineRun.getRunId());

        // Verify that a maneuver run created for this replay transitions to FAILED upon failure
        SimulationRun failedManeuverRun = new SimulationRun(
                "RUN-MAN-FAIL", scenario, baselineRun.getRunId(), cand.getManeuverCandidateId());
        failedManeuverRun.start();
        failedManeuverRun.fail();
        assertEquals(SimulationRun.Status.FAILED, failedManeuverRun.getStatus());
        assertEquals("RUN-BASE-FAIL", failedManeuverRun.getBaselineRunId());
        assertEquals("MAN-01", failedManeuverRun.getManeuverCandidateId());
    }

    @Test
    void failsManeuverRunAndPreservesBaselineWhenTargetIsMissingAtManeuverTick() {
        InitialObjectState deb1 = new InitialObjectState(
                "DEB-1",
                SpaceObject.ObjectType.DEBRIS,
                new Vector3D(-100.0, 0.0, 0.0),
                new Vector3D(1.0, 0.0, 0.0),
                1.0,
                SpaceObject.Status.ACTIVE,
                null,
                null
        );
        InitialObjectState deb2 = new InitialObjectState(
                "DEB-2",
                SpaceObject.ObjectType.DEBRIS,
                new Vector3D(100.0, 0.0, 0.0),
                new Vector3D(-1.0, 0.0, 0.0),
                1.0,
                SpaceObject.Status.ACTIVE,
                null,
                null
        );
        Scenario scenario = new Scenario(
                "SCENARIO-MISSING",
                new ScenarioGenerationConfig(0, 2, -100, 100, -1, 1, 1.0, 1.0),
                42L,
                List.of(deb1, deb2),
                Scenario.Status.READY
        );

        SimulationRun baselineRun = createBaselineRun("RUN-BASE-MISSING", scenario);
        baselineRun.start();

        // Target SAT-1 belongs to encounter pair, but is missing from the scenario
        Encounter baselineEnc = createAssessedEncounter("ENC-MISSING", baselineRun.getRunId(), "SAT-1", "DEB-1", 5);
        Vector3D dv = new Vector3D(0, 0, 0.010);
        ManeuverCandidate cand = new ManeuverCandidate("MAN-MISSING", baselineEnc.getEncounterId(), "SAT-1", dv, 15.0, 0.010, 1);

        ManeuverReplayService service = new ManeuverReplayService();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.replayAndApply("RUN-MAN-MISSING", baselineRun, baselineEnc, cand));

        assertTrue(ex.getMessage().contains("not found"));

        // Baseline encounter remains ASSESSED (not MITIGATED)
        assertEquals(Encounter.Status.ASSESSED, baselineEnc.getStatus());

        // Baseline run remains in its valid state
        assertEquals(SimulationRun.Status.RUNNING, baselineRun.getStatus());
        assertEquals("RUN-BASE-MISSING", baselineRun.getRunId());

        // Verify that a maneuver run created for this replay transitions to FAILED upon failure
        SimulationRun failedManeuverRun = new SimulationRun(
                "RUN-MAN-MISSING", scenario, baselineRun.getRunId(), cand.getManeuverCandidateId());
        failedManeuverRun.start();
        failedManeuverRun.fail();
        assertEquals(SimulationRun.Status.FAILED, failedManeuverRun.getStatus());
        assertEquals("RUN-BASE-MISSING", failedManeuverRun.getBaselineRunId());
        assertEquals("MAN-MISSING", failedManeuverRun.getManeuverCandidateId());
    }

    @Test
    void populatesRecalculatedEncountersWithNewRunIdAndAssessedRiskScores() {
        Scenario scenario = createKnownThreatScenario();
        SimulationRun baselineRun = createBaselineRun("RUN-BASE-RECALC", scenario);
        baselineRun.start();

        Encounter baselineEnc = createAssessedEncounter("ENC-ORIG", baselineRun.getRunId(), "SAT-1", "DEB-1", 10);
        // Apply very small delta-v: 0.001 km/s in Z.
        // SAT-1 and DEB-1 still pass very close (~0.1 km) at t=100s, so encounter is detected in maneuver run.
        Vector3D smallDeltaV = new Vector3D(0.0, 0.0, 0.001);
        ManeuverCandidate cand = new ManeuverCandidate("MAN-SMALL", baselineEnc.getEncounterId(), "SAT-1", smallDeltaV, 0.1, 0.001, 1);

        ManeuverReplayService service = new ManeuverReplayService();
        ManeuverReplayResult result = service.replayAndApply("RUN-MAN-RECALC", baselineRun, baselineEnc, cand);

        List<Encounter> recalculated = result.getRecalculatedEncounters();
        assertFalse(recalculated.isEmpty(), "Expected at least one recalculated encounter in maneuver run.");

        for (Encounter rec : recalculated) {
            // Must belong to the NEW run ID
            assertEquals("RUN-MAN-RECALC", rec.getRunId());
            // Must NOT leak the baseline encounter ID
            assertNotEquals("ENC-ORIG", rec.getEncounterId());
            // Detection tick must be valid
            assertTrue(rec.getDetectedAtTick() >= 0);
            assertTrue(rec.getDetectedAtTick() <= 720);
            // Valid measurements
            assertTrue(rec.getMinSeparationKm() >= 0.0);
            assertTrue(rec.getMinSeparationKm() <= EncounterDetectionService.DETECTION_DISTANCE_KM);
            assertTrue(rec.getTimeToClosestApproachSeconds() >= 0.0);
            assertTrue(rec.getTimeToClosestApproachSeconds() <= SimulationConstants.SIMULATION_HORIZON_SECONDS);
            assertTrue(rec.getRelativeVelocityKmPerSec() >= 0.0);
            // Must have been assessed with a valid RiskScore (and subsequently resolved once separated)
            assertEquals(Encounter.Status.RESOLVED, rec.getStatus());
            assertNotNull(rec.getRiskScore());
            assertTrue(rec.getRiskScore().getTotalScore() >= 0);
            assertTrue(rec.getRiskScore().getTotalScore() <= 100);
            assertNotNull(rec.getRiskScore().getExplanation());
            assertNotNull(rec.getRiskScore().getLabel());
        }
    }
}
