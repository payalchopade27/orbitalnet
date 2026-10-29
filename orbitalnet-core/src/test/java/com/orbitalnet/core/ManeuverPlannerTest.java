package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ManeuverPlannerTest {

    private Satellite createSatellite(String id, Vector3D pos, Vector3D vel) {
        return new Satellite(
                id,
                "Sat-" + id,
                pos,
                vel,
                0.010, // 10 m radius (0.010 km)
                SpaceObject.Status.ACTIVE,
                null
        );
    }

    private Debris createDebris(String id, Vector3D pos, Vector3D vel) {
        return new Debris(
                id,
                pos,
                vel,
                0.005, // 5 m radius (0.005 km)
                SpaceObject.Status.ACTIVE
        );
    }

    private Encounter createEncounter(
            String id,
            String satId,
            String debId,
            double separation,
            double time,
            double relSpeed) {

        Encounter enc = new Encounter(
                id, "RUN-01", satId, debId,
                separation, time, relSpeed, 10
        );
        RiskScore score = new RiskScore(
                85, 0.85, 0.50, 0.80,
                RiskLabel.CRITICAL, "Critical risk"
        );
        enc.assess(score);
        return enc;
    }

    @Test
    void maneuverCandidateModelValidatesInputsAndStoresProperties() {
        Vector3D dv = new Vector3D(0.005, 0.0, 0.010);
        double cost = dv.magnitude();
        ManeuverCandidate candidate = new ManeuverCandidate(
                "MC-01", "ENC-01", "SAT-01",
                dv, 25.5, cost, 1
        );

        assertEquals("MC-01", candidate.getManeuverCandidateId());
        assertEquals("ENC-01", candidate.getEncounterId());
        assertEquals("SAT-01", candidate.getTargetObjectId());
        assertEquals(dv, candidate.getDeltaV());
        assertEquals(25.5, candidate.getNewMinSeparationKm(), 0.000001);
        assertEquals(cost, candidate.getCost(), 0.000001);
        assertEquals(1, candidate.getRank());

        // Equivalence
        ManeuverCandidate same = new ManeuverCandidate(
                "MC-01", "ENC-01", "SAT-01",
                dv, 25.5, cost, 1
        );
        assertEquals(candidate, same);
        assertEquals(candidate.hashCode(), same.hashCode());
        assertTrue(candidate.toString().contains("MC-01"));

        // Validation failures
        assertThrows(IllegalArgumentException.class, () ->
                new ManeuverCandidate("", "ENC-01", "SAT-01", dv, 25.5, cost, 1));
        assertThrows(IllegalArgumentException.class, () ->
                new ManeuverCandidate("MC-01", "", "SAT-01", dv, 25.5, cost, 1));
        assertThrows(IllegalArgumentException.class, () ->
                new ManeuverCandidate("MC-01", "ENC-01", "", dv, 25.5, cost, 1));
        assertThrows(NullPointerException.class, () ->
                new ManeuverCandidate("MC-01", "ENC-01", "SAT-01", null, 25.5, cost, 1));
        assertThrows(IllegalArgumentException.class, () ->
                new ManeuverCandidate("MC-01", "ENC-01", "SAT-01", dv, -1.0, cost, 1));
        assertThrows(IllegalArgumentException.class, () ->
                new ManeuverCandidate("MC-01", "ENC-01", "SAT-01", dv, 25.5, -0.5, 1));
        assertThrows(IllegalArgumentException.class, () ->
                new ManeuverCandidate("MC-01", "ENC-01", "SAT-01", dv, 25.5, cost, 0));
    }

    @Test
    void plansCandidatesDeterministicallyWithoutMutatingBaselineObjects() {
        Satellite sat = createSatellite("SAT-1", new Vector3D(0, 0, 0), new Vector3D(7.5, 0, 0));
        Debris deb = createDebris("DEB-1", new Vector3D(50, 0, 0), new Vector3D(-7.5, 0, 0));
        Encounter enc = createEncounter("ENC-01", sat.getId(), deb.getId(), 0.0, 3.33, 15.0);

        Vector3D origSatPos = sat.getPosition();
        Vector3D origSatVel = sat.getVelocity();
        Vector3D origDebPos = deb.getPosition();
        Vector3D origDebVel = deb.getVelocity();

        ManeuverPlanner planner = new ManeuverPlanner();
        List<ManeuverCandidate> plan1 = planner.planManeuvers(enc, sat, deb);
        List<ManeuverCandidate> plan2 = planner.planManeuvers(enc, sat, deb);

        // Verification of determinism
        assertEquals(3, plan1.size());
        assertEquals(plan1, plan2);

        // Baseline objects must remain strictly unchanged
        assertEquals(origSatPos, sat.getPosition());
        assertEquals(origSatVel, sat.getVelocity());
        assertEquals(origDebPos, deb.getPosition());
        assertEquals(origDebVel, deb.getVelocity());

        // Verify ranks are unique 1, 2, 3
        assertEquals(1, plan1.get(0).getRank());
        assertEquals(2, plan1.get(1).getRank());
        assertEquals(3, plan1.get(2).getRank());
    }

    @Test
    void knownThreatScenarioGeneratesFeasibleClearanceManeuver() {
        // Use the fixed-seed known threat scenario
        ScenarioGenerator generator = new ScenarioGenerator();
        Scenario scenario = generator.generateKnownThreatScenario("KNOWN-THREAT-1", 42L);

        SpaceObject sat = scenario.getInitialObjectStates().get(0).toSpaceObject();
        SpaceObject deb = scenario.getInitialObjectStates().get(1).toSpaceObject();

        assertEquals("SAT-1", sat.getId());
        assertEquals("DEB-1", deb.getId());

        // Baseline head-on approach: separation is 0.0 km at t = 100s
        ClosestApproach baselineCA = new ClosestApproach(
                sat.getPosition(), sat.getVelocity(),
                deb.getPosition(), deb.getVelocity()
        );
        assertEquals(0.0, baselineCA.getDistanceKm(), 0.000001);
        assertEquals(100.0, baselineCA.getTimeSeconds(), 0.000001);

        Encounter enc = createEncounter("ENC-HEAD-ON", sat.getId(), deb.getId(), 0.0, 100.0, 2.0);

        ManeuverPlanner planner = new ManeuverPlanner();
        List<ManeuverCandidate> candidates = planner.planManeuvers(enc, sat, deb);

        assertEquals(3, candidates.size());

        // Rank 1 must improve separation from 0.0 km
        ManeuverCandidate best = candidates.get(0);
        assertEquals(1, best.getRank());
        assertTrue(best.getNewMinSeparationKm() > 0.0,
                "Expected maneuver to increase separation above 0 km, got: " + best.getNewMinSeparationKm());

        // Verify safety-first ranking: candidate 1 separation >= candidate 2 separation >= candidate 3
        assertTrue(candidates.get(0).getNewMinSeparationKm() >= candidates.get(1).getNewMinSeparationKm());
        assertTrue(candidates.get(1).getNewMinSeparationKm() >= candidates.get(2).getNewMinSeparationKm());
    }

    @Test
    void safetyFirstRankingWithDeterministicTieBreaks() {
        Satellite sat = createSatellite("SAT-A", new Vector3D(0, 0, 0), new Vector3D(1, 0, 0));
        Debris deb = createDebris("DEB-A", new Vector3D(100, 0, 0), new Vector3D(-1, 0, 0));
        Encounter enc = createEncounter("ENC-TIE", sat.getId(), deb.getId(), 0.0, 50.0, 2.0);

        ManeuverPlanner planner = new ManeuverPlanner();

        // Custom candidate delta-vs:
        // dv1: small separation
        // dv2: large separation, cost 0.02
        // dv3: large separation (identical separation to dv2), cost 0.01 (lower cost should win tie)
        Vector3D dv1 = new Vector3D(0.01, 0, 0); // In-plane, 0 km miss
        Vector3D dv2 = new Vector3D(0, 0, 0.02); // Out-of-plane, large miss, cost 0.02
        Vector3D dv3 = new Vector3D(0, 0, 0.01); // Out-of-plane, cost 0.01

        List<ManeuverCandidate> ranked = planner.planManeuvers(enc, sat, deb, Arrays.asList(dv1, dv2, dv3));

        assertEquals(3, ranked.size());
        assertEquals(1, ranked.get(0).getRank());
        assertEquals(2, ranked.get(1).getRank());
        assertEquals(3, ranked.get(2).getRank());

        // Highest separation is dv2 (at 0.02 km/s impulse)
        assertTrue(ranked.get(0).getNewMinSeparationKm() > ranked.get(1).getNewMinSeparationKm());
        assertTrue(ranked.get(1).getNewMinSeparationKm() > ranked.get(2).getNewMinSeparationKm());
    }

    @Test
    void validationRejectsInvalidAssociationsAndDebrisTarget() {
        Satellite sat = createSatellite("SAT-01", new Vector3D(0, 0, 0), new Vector3D(1, 0, 0));
        Debris deb = createDebris("DEB-01", new Vector3D(10, 0, 0), new Vector3D(0, 1, 0));
        Satellite outsider = createSatellite("SAT-OUTSIDER", new Vector3D(100, 0, 0), new Vector3D(0, 0, 1));
        Encounter enc = createEncounter("ENC-01", sat.getId(), deb.getId(), 5.0, 50.0, 1.414);

        ManeuverPlanner planner = new ManeuverPlanner();

        // Null checks
        assertThrows(NullPointerException.class, () -> planner.planManeuvers(null, sat, deb));
        assertThrows(NullPointerException.class, () -> planner.planManeuvers(enc, null, deb));
        assertThrows(NullPointerException.class, () -> planner.planManeuvers(enc, sat, null));

        // Outsider object not in encounter
        assertThrows(IllegalArgumentException.class, () -> planner.planManeuvers(enc, outsider, deb));
        assertThrows(IllegalArgumentException.class, () -> planner.planManeuvers(enc, sat, outsider));

        // Same object passed twice
        assertThrows(IllegalArgumentException.class, () -> planner.planManeuvers(enc, sat, sat));

        // Debris cannot maneuver when satellite is present
        assertThrows(IllegalArgumentException.class, () -> planner.planManeuvers(enc, deb, sat));

        // Inactive satellite cannot maneuver
        Satellite inactiveSat = new Satellite(
                "SAT-01", "Sat-01",
                new Vector3D(0, 0, 0), new Vector3D(1, 0, 0),
                0.01, SpaceObject.Status.INACTIVE, null
        );
        assertThrows(IllegalArgumentException.class, () -> planner.planManeuvers(enc, inactiveSat, deb));
    }

    @Test
    void shouldHandleZeroVelocityTargetWithDeterministicCandidates() {
        Satellite stationarySat = createSatellite("SAT-ZERO", new Vector3D(0, 0, 0), new Vector3D(0, 0, 0));
        Debris movingDeb = createDebris("DEB-MOV", new Vector3D(100, 0, 0), new Vector3D(-1, 0, 0));
        Encounter enc = createEncounter("ENC-ZERO", stationarySat.getId(), movingDeb.getId(), 0.0, 100.0, 1.0);

        ManeuverPlanner planner = new ManeuverPlanner();
        List<ManeuverCandidate> candidates = planner.planManeuvers(enc, stationarySat, movingDeb);

        assertEquals(3, candidates.size());
        for (ManeuverCandidate c : candidates) {
            Vector3D dv = c.getDeltaV();
            assertTrue(Double.isFinite(dv.getX()));
            assertTrue(Double.isFinite(dv.getY()));
            assertTrue(Double.isFinite(dv.getZ()));
            assertEquals(0.010, dv.magnitude(), 0.000001);
            assertTrue(Double.isFinite(c.getNewMinSeparationKm()));
            assertTrue(c.getNewMinSeparationKm() >= 0.0);
        }

        // Deterministic repeat
        List<ManeuverCandidate> repeat = planner.planManeuvers(enc, stationarySat, movingDeb);
        assertEquals(candidates, repeat);
    }

    @Test
    void shouldProduceOrthogonalVectorsAcrossReferenceAxisThreshold() {
        // Case A: |progradeUnit.x| < 0.9 (velocity along Y axis: (0, 7.5, 0))
        Vector3D velA = new Vector3D(0, 7.5, 0);
        List<Vector3D> vectorsA = ManeuverPlanner.generateDeterministicDeltaVs(velA, new Vector3D(100, 0, 0), 0.010);
        assertEquals(3, vectorsA.size());
        Vector3D normA = vectorsA.get(0);
        Vector3D radA = vectorsA.get(1);
        Vector3D proA = vectorsA.get(2);

        assertEquals(0.010, normA.magnitude(), 0.000001);
        assertEquals(0.010, radA.magnitude(), 0.000001);
        assertEquals(0.010, proA.magnitude(), 0.000001);

        assertEquals(0.0, normA.dot(proA), 0.000001, "Normal and Prograde must be orthogonal");
        assertEquals(0.0, radA.dot(proA), 0.000001, "Radial and Prograde must be orthogonal");
        assertEquals(0.0, radA.dot(normA), 0.000001, "Radial and Normal must be orthogonal");

        // Case B: |progradeUnit.x| >= 0.9 (velocity primarily along X axis: (7.5, 0, 0))
        Vector3D velB = new Vector3D(7.5, 0, 0);
        List<Vector3D> vectorsB = ManeuverPlanner.generateDeterministicDeltaVs(velB, new Vector3D(0, 100, 0), 0.010);
        assertEquals(3, vectorsB.size());
        Vector3D normB = vectorsB.get(0);
        Vector3D radB = vectorsB.get(1);
        Vector3D proB = vectorsB.get(2);

        assertEquals(0.010, normB.magnitude(), 0.000001);
        assertEquals(0.010, radB.magnitude(), 0.000001);
        assertEquals(0.010, proB.magnitude(), 0.000001);

        assertEquals(0.0, normB.dot(proB), 0.000001, "Normal and Prograde must be orthogonal for X-dominant vel");
        assertEquals(0.0, radB.dot(proB), 0.000001, "Radial and Prograde must be orthogonal for X-dominant vel");
        assertEquals(0.0, radB.dot(normB), 0.000001, "Radial and Normal must be orthogonal for X-dominant vel");
    }

    @Test
    void shouldRankLowerCostFirstWhenSeparationIsIdentical() {
        Satellite sat = createSatellite("SAT-01", new Vector3D(0, 0, 0), new Vector3D(1.0, 0, 0));
        Debris deb = createDebris("DEB-01", new Vector3D(500, 0, 0), new Vector3D(10.0, 0, 0));
        Encounter enc = createEncounter("ENC-COST-TIE", sat.getId(), deb.getId(), 500.0, 0.0, 9.0);

        // Both delta-vs maintain r . v_rel > 0 (diverging), so t* clamps to 0.0 s.
        // Both yield identical closest approach separation of 500.0 km.
        // dvHigh has cost 0.50 km/s, dvLow has cost 0.10 km/s.
        Vector3D dvHigh = new Vector3D(0.50, 0, 0);
        Vector3D dvLow = new Vector3D(0.10, 0, 0);

        ManeuverPlanner planner = new ManeuverPlanner();
        List<ManeuverCandidate> ranked = planner.planManeuvers(
                enc, sat, deb, Arrays.asList(dvHigh, dvLow)
        );

        assertEquals(2, ranked.size());
        assertEquals(500.0, ranked.get(0).getNewMinSeparationKm(), 0.000001);
        assertEquals(500.0, ranked.get(1).getNewMinSeparationKm(), 0.000001);

        // Lower cost (0.10) must rank 1, higher cost (0.50) must rank 2
        assertEquals(1, ranked.get(0).getRank());
        assertEquals(0.10, ranked.get(0).getCost(), 0.000001);

        assertEquals(2, ranked.get(1).getRank());
        assertEquals(0.50, ranked.get(1).getCost(), 0.000001);
    }

    @Test
    void shouldRankLexicographicallySmallerIdFirstWhenSeparationAndCostAreEqual() {
        Satellite sat = createSatellite("SAT-01", new Vector3D(0, 0, 0), new Vector3D(1.0, 0, 0));
        Debris deb = createDebris("DEB-01", new Vector3D(500, 0, 0), new Vector3D(10.0, 0, 0));
        Encounter enc = createEncounter("ENC-ID-TIE", sat.getId(), deb.getId(), 500.0, 0.0, 9.0);

        // dv1 = (0, 0.1, 0) and dv2 = (0, -0.1, 0)
        // Both maintain diverging geometry: separation = 500.0 km, cost = 0.10 km/s.
        Vector3D dv1 = new Vector3D(0, 0.10, 0);
        Vector3D dv2 = new Vector3D(0, -0.10, 0);

        ManeuverPlanner planner = new ManeuverPlanner();
        List<ManeuverCandidate> ranked = planner.planManeuvers(
                enc, sat, deb, Arrays.asList(dv1, dv2)
        );

        assertEquals(2, ranked.size());
        assertEquals(500.0, ranked.get(0).getNewMinSeparationKm(), 0.000001);
        assertEquals(500.0, ranked.get(1).getNewMinSeparationKm(), 0.000001);
        assertEquals(0.10, ranked.get(0).getCost(), 0.000001);
        assertEquals(0.10, ranked.get(1).getCost(), 0.000001);

        // Lexicographically smaller candidateId (...-MAN-01 before ...-MAN-02) must take rank 1
        assertEquals("ENC-ID-TIE-MAN-01", ranked.get(0).getManeuverCandidateId());
        assertEquals(1, ranked.get(0).getRank());

        assertEquals("ENC-ID-TIE-MAN-02", ranked.get(1).getManeuverCandidateId());
        assertEquals(2, ranked.get(1).getRank());
    }

    @Test
    void shouldReturnEmptyListWhenCustomCandidateListIsEmpty() {
        Satellite sat = createSatellite("SAT-01", new Vector3D(0, 0, 0), new Vector3D(1, 0, 0));
        Debris deb = createDebris("DEB-01", new Vector3D(10, 0, 0), new Vector3D(-1, 0, 0));
        Encounter enc = createEncounter("ENC-EMPTY", sat.getId(), deb.getId(), 10.0, 5.0, 2.0);

        ManeuverPlanner planner = new ManeuverPlanner();
        List<ManeuverCandidate> result = planner.planManeuvers(enc, sat, deb, List.of());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldPreserveEncounterMeasurementsAndStatusAcrossPlanning() {
        Satellite sat = createSatellite("SAT-01", new Vector3D(0, 0, 0), new Vector3D(7.5, 0, 0));
        Debris deb = createDebris("DEB-01", new Vector3D(100, 0, 0), new Vector3D(-7.5, 0, 0));
        Encounter enc = createEncounter("ENC-SNAPSHOT", sat.getId(), deb.getId(), 12.5, 450.0, 15.0);

        // Snapshot all encounter fields before planning
        double snapshotSep = enc.getMinSeparationKm();
        double snapshotTime = enc.getTimeToClosestApproachSeconds();
        double snapshotRelVel = enc.getRelativeVelocityKmPerSec();
        int snapshotTick = enc.getDetectedAtTick();
        Encounter.Status snapshotStatus = enc.getStatus();
        RiskScore snapshotRisk = enc.getRiskScore();

        ManeuverPlanner planner = new ManeuverPlanner();
        planner.planManeuvers(enc, sat, deb);

        // Assert all encounter fields are strictly identical afterward
        assertEquals(snapshotSep, enc.getMinSeparationKm(), 0.000001);
        assertEquals(snapshotTime, enc.getTimeToClosestApproachSeconds(), 0.000001);
        assertEquals(snapshotRelVel, enc.getRelativeVelocityKmPerSec(), 0.000001);
        assertEquals(snapshotTick, enc.getDetectedAtTick());
        assertEquals(snapshotStatus, enc.getStatus());
        assertSame(snapshotRisk, enc.getRiskScore());
    }

    @Test
    void shouldRejectInvalidDeltaVMagnitudes() {
        Vector3D vel = new Vector3D(1, 0, 0);
        Vector3D pos = new Vector3D(100, 0, 0);

        assertThrows(IllegalArgumentException.class, () ->
                ManeuverPlanner.generateDeterministicDeltaVs(vel, pos, 0.0));
        assertThrows(IllegalArgumentException.class, () ->
                ManeuverPlanner.generateDeterministicDeltaVs(vel, pos, -0.01));
        assertThrows(IllegalArgumentException.class, () ->
                ManeuverPlanner.generateDeterministicDeltaVs(vel, pos, Double.NaN));
        assertThrows(IllegalArgumentException.class, () ->
                ManeuverPlanner.generateDeterministicDeltaVs(vel, pos, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () ->
                ManeuverPlanner.generateDeterministicDeltaVs(vel, pos, Double.NEGATIVE_INFINITY));

        Satellite sat = createSatellite("SAT-01", pos, vel);
        Debris deb = createDebris("DEB-01", new Vector3D(200, 0, 0), new Vector3D(-1, 0, 0));
        Encounter enc = createEncounter("ENC-MAG", sat.getId(), deb.getId(), 50.0, 50.0, 2.0);
        ManeuverPlanner planner = new ManeuverPlanner();

        assertThrows(IllegalArgumentException.class, () ->
                planner.planManeuvers(enc, sat, deb, 0.0));
        assertThrows(IllegalArgumentException.class, () ->
                planner.planManeuvers(enc, sat, deb, -0.05));
    }
}

