package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EncounterTrackerTest {

    private final EncounterDetectionService detectionService =
            new EncounterDetectionService();

    private List<EncounterDetectionService.DetectedEncounter> detectPair(
            String firstId, String secondId) {

        SpaceObject first = new Debris(
                firstId,
                new Vector3D(0, 0, 0),
                new Vector3D(1, 0, 0),
                1.0,
                SpaceObject.Status.ACTIVE);

        SpaceObject second = new Debris(
                secondId,
                new Vector3D(10, 0, 0),
                new Vector3D(-1, 0, 0),
                1.0,
                SpaceObject.Status.ACTIVE);

        return detectionService.detect(List.of(first, second));
    }


    private List<SpaceObject> currentObjects(
            String firstId, String secondId) {

        SpaceObject first = new Debris(
                firstId,
                new Vector3D(0, 0, 0),
                new Vector3D(1, 0, 0),
                1.0,
                SpaceObject.Status.ACTIVE);

        SpaceObject second = new Debris(
                secondId,
                new Vector3D(10, 0, 0),
                new Vector3D(-1, 0, 0),
                1.0,
                SpaceObject.Status.ACTIVE);

        return List.of(first, second);
    }


    private List<SpaceObject> separatedObjectsMovingApart(
            String firstId, String secondId) {

        SpaceObject first = new Debris(
                firstId,
                new Vector3D(0, 0, 0),
                new Vector3D(-1, 0, 0),
                1.0,
                SpaceObject.Status.ACTIVE);

        SpaceObject second = new Debris(
                secondId,
                new Vector3D(150, 0, 0),
                new Vector3D(1, 0, 0),
                1.0,
                SpaceObject.Status.ACTIVE);

        return List.of(first, second);
    }


    @Test
    void consecutiveDetectionsUpdateSameEncounter() {
        EncounterTracker tracker = new EncounterTracker("RUN-1");

        Encounter first = tracker.processTick(
                0, detectPair("A", "B"), currentObjects("A", "B")).get(0);

        tracker.processTick(1, detectPair("A", "B"), currentObjects("A", "B"));

        assertEquals(1, tracker.getEncounters().size());
        assertEquals(first.getEncounterId(),
                tracker.getEncounters().get(0).getEncounterId());
        assertEquals(0, first.getDetectedAtTick());
    }


    @Test
    void detectionAfterOneMissedTickContinuesSameEncounter() {
        EncounterTracker tracker = new EncounterTracker("RUN-1");

        Encounter first = tracker.processTick(
                0, detectPair("A", "B"), currentObjects("A", "B")).get(0);

        tracker.processTick(1, Collections.emptyList(), currentObjects("A", "B"));

        Encounter second = tracker.processTick(
                2, detectPair("A", "B"), currentObjects("A", "B")).get(0);

        assertEquals(1, tracker.getEncounters().size());
        assertEquals(first.getEncounterId(), second.getEncounterId());
        assertEquals(0, second.getDetectedAtTick());
    }


    @Test
    void detectionAfterTwoConsecutiveMissedTicksCreatesNewEncounter() {
        EncounterTracker tracker = new EncounterTracker("RUN-1");

        Encounter first = tracker.processTick(
                0, detectPair("A", "B"), currentObjects("A", "B")).get(0);

        tracker.processTick(1, Collections.emptyList(), currentObjects("A", "B"));
        tracker.processTick(2, Collections.emptyList(), currentObjects("A", "B"));

        Encounter second = tracker.processTick(
                3, detectPair("A", "B"), currentObjects("A", "B")).get(0);

        assertEquals(2, tracker.getEncounters().size());
        assertNotEquals(first.getEncounterId(), second.getEncounterId());
        assertEquals(0, first.getDetectedAtTick());
        assertEquals(3, second.getDetectedAtTick());
    }


    @Test
    void reversedObjectOrderContinuesSameEncounter() {
        EncounterTracker tracker = new EncounterTracker("RUN-1");

        Encounter first = tracker.processTick(
                0, detectPair("A", "B"), currentObjects("A", "B")).get(0);

        tracker.processTick(1, detectPair("B", "A"), currentObjects("A", "B"));

        assertEquals(1, tracker.getEncounters().size());
        assertEquals("A", first.getObjectAId());
        assertEquals("B", first.getObjectBId());
    }

    @Test
    void resolvedEncounterIsRetainedAndNotReopened() {
        EncounterTracker tracker = new EncounterTracker("RUN-1");

        Encounter first = tracker.processTick(
                0, detectPair("A", "B"), currentObjects("A", "B")).get(0);

        first.assess();
        first.resolve();

        tracker.processTick(1, Collections.emptyList(), currentObjects("A", "B"));

        Encounter second = tracker.processTick(
                2, detectPair("A", "B"), currentObjects("A", "B")).get(0);

        assertEquals(2, tracker.getEncounters().size());
        assertEquals(Encounter.Status.RESOLVED, first.getStatus());
        assertNotEquals(first.getEncounterId(), second.getEncounterId());
        assertEquals(Encounter.Status.DETECTED, second.getStatus());
    }

    @Test
    void ticksMustIncreaseStrictly() {
        EncounterTracker tracker = new EncounterTracker("RUN-1");

        tracker.processTick(0, Collections.emptyList(), currentObjects("A", "B"));

        assertThrows(IllegalArgumentException.class,
                () -> tracker.processTick(0, Collections.emptyList(), currentObjects("A", "B")));

        assertThrows(IllegalArgumentException.class,
                () -> tracker.processTick(-1, Collections.emptyList(), currentObjects("A", "B")));
    }


    @Test
    void encounterResolvesAfterTwoMissedTicksWhenObjectsMoveApart() {
        EncounterTracker tracker = new EncounterTracker("run-1");

        tracker.processTick(
                0,
                detectPair("A", "B"),
                currentObjects("A", "B"));

        Encounter encounter = tracker.getEncounters().get(0);

        tracker.processTick(
                1,
                List.of(),
                separatedObjectsMovingApart("A", "B"));

        tracker.processTick(
                2,
                List.of(),
                separatedObjectsMovingApart("A", "B"));

        assertEquals(Encounter.Status.RESOLVED, encounter.getStatus());
    }


    @Test
    void encounterDoesNotResolveWhenObjectsAreStillMovingTowardEachOther() {
        EncounterTracker tracker = new EncounterTracker("run-1");

        tracker.processTick(
                0,
                detectPair("A", "B"),
                currentObjects("A", "B"));

        Encounter encounter = tracker.getEncounters().get(0);

        tracker.processTick(
                1,
                List.of(),
                currentObjects("A", "B"));

        tracker.processTick(
                2,
                List.of(),
                currentObjects("A", "B"));

        assertNotEquals(Encounter.Status.RESOLVED, encounter.getStatus());
    }

    @Test
    void shouldRejectDuplicateDetectionsForSamePairInSameTick() {
        EncounterTracker tracker = new EncounterTracker("RUN-1");

        EncounterDetectionService.DetectedEncounter det1 = detectPair("A", "B").get(0);
        EncounterDetectionService.DetectedEncounter det2 = detectPair("A", "B").get(0);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tracker.processTick(0, List.of(det1, det2), currentObjects("A", "B"))
        );
        assertEquals("Duplicate detection for the same object pair in one tick.", exception.getMessage());

        // Also verify when duplicate detection has reversed object order
        EncounterDetectionService.DetectedEncounter detForward = detectPair("A", "B").get(0);
        EncounterDetectionService.DetectedEncounter detReversed = detectPair("B", "A").get(0);

        IllegalArgumentException reversedException = assertThrows(
                IllegalArgumentException.class,
                () -> tracker.processTick(0, List.of(detForward, detReversed), currentObjects("A", "B"))
        );
        assertEquals("Duplicate detection for the same object pair in one tick.", reversedException.getMessage());
    }

}
