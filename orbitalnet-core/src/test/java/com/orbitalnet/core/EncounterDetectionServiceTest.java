
package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EncounterDetectionServiceTest {

    private final EncounterDetectionService service =
            new EncounterDetectionService();

    private Satellite satellite(
            String id, double x, double vx, SpaceObject.Status status) {

        return new Satellite(
                id,
                "Test Satellite",
                new Vector3D(x, 0, 0),
                new Vector3D(vx, 0, 0),
                1.0,
                status,
                null
        );
    }

    private Debris debris(
            String id, double x, double vx, SpaceObject.Status status) {

        return new Debris(
                id,
                new Vector3D(x, 0, 0),
                new Vector3D(vx, 0, 0),
                1.0,
                status
        );
    }

    @Test
    void shouldDetectObjectsWhoseClosestApproachIsWithin100Km() {
        List<EncounterDetectionService.DetectedEncounter> results =
                service.detect(Arrays.asList(
                        satellite("SAT-01", -100, 1, SpaceObject.Status.ACTIVE),
                        debris("DEB-01", 100, -1, SpaceObject.Status.ACTIVE)
                ));

        assertEquals(1, results.size());
        assertEquals(0.0, results.get(0).getMinSeparationKm(), 0.000001);
        assertEquals(100.0,
                results.get(0).getTimeToClosestApproachSeconds(), 0.000001);
        assertEquals(2.0,
                results.get(0).getRelativeVelocityKmPerSec(), 0.000001);
        assertTrue(results.get(0).hasCollisionCondition());
    }

    @Test
    void shouldDetectExactlyAt100Km() {
        List<EncounterDetectionService.DetectedEncounter> results =
                service.detect(Arrays.asList(
                        satellite("SAT-01", 0, 0, SpaceObject.Status.ACTIVE),
                        debris("DEB-01", 100, 0, SpaceObject.Status.ACTIVE)
                ));

        assertEquals(1, results.size());
        assertEquals(100.0, results.get(0).getMinSeparationKm(), 0.000001);
        assertFalse(results.get(0).hasCollisionCondition());
    }

    @Test
    void shouldNotDetectObjectsBeyond100Km() {
        List<EncounterDetectionService.DetectedEncounter> results =
                service.detect(Arrays.asList(
                        satellite("SAT-01", 0, 0, SpaceObject.Status.ACTIVE),
                        debris("DEB-01", 101, 0, SpaceObject.Status.ACTIVE)
                ));

        assertTrue(results.isEmpty());
    }

    @Test
    void shouldIgnoreInactiveObjects() {
        List<EncounterDetectionService.DetectedEncounter> results =
                service.detect(Arrays.asList(
                        satellite("SAT-01", 0, 0, SpaceObject.Status.ACTIVE),
                        debris("DEB-01", 10, 0, SpaceObject.Status.INACTIVE),
                        debris("DEB-02", 20, 0, SpaceObject.Status.DESTROYED)
                ));

        assertTrue(results.isEmpty());
    }

    @Test
    void shouldCheckEachPairOnlyOnce() {
        List<EncounterDetectionService.DetectedEncounter> results =
                service.detect(Arrays.asList(
                        satellite("SAT-01", 0, 0, SpaceObject.Status.ACTIVE),
                        debris("DEB-01", 10, 0, SpaceObject.Status.ACTIVE),
                        debris("DEB-02", 20, 0, SpaceObject.Status.ACTIVE)
                ));

        assertEquals(3, results.size());
    }

    @Test
    void shouldReturnNoResultsForEmptyInput() {
        assertTrue(service.detect(Collections.emptyList()).isEmpty());
    }
}
