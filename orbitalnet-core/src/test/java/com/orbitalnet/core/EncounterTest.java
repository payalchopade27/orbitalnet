package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncounterTest {

    private Encounter createEncounter() {
        return new Encounter(
                "ENC-01",
                "RUN-01",
                "SAT-01",
                "DEB-01",
                20.0,
                100.0,
                2.0,
                5
        );
    }

    @Test
    void shouldStoreObjectIdsInCanonicalOrder() {
        Encounter encounter = new Encounter(
                "ENC-01", "RUN-01",
                "SAT-01", "DEB-01",
                20.0, 100.0, 2.0, 5
        );

        assertEquals("DEB-01", encounter.getObjectAId());
        assertEquals("SAT-01", encounter.getObjectBId());
    }

    @Test
    void shouldStartWithDetectedStatus() {
        Encounter encounter = createEncounter();

        assertEquals(Encounter.Status.DETECTED, encounter.getStatus());
        assertEquals(5, encounter.getDetectedAtTick());
    }

    @Test
    void shouldUpdateMeasurementsWithoutChangingDetectionTick() {
        Encounter encounter = createEncounter();

        encounter.updateMeasurements(15.0, 80.0, 3.0);

        assertEquals(15.0, encounter.getMinSeparationKm());
        assertEquals(80.0, encounter.getTimeToClosestApproachSeconds());
        assertEquals(3.0, encounter.getRelativeVelocityKmPerSec());
        assertEquals(5, encounter.getDetectedAtTick());
    }

    @Test
    void shouldFollowValidLifecycle() {
        Encounter encounter = createEncounter();

        encounter.assess();
        assertEquals(Encounter.Status.ASSESSED, encounter.getStatus());

        encounter.mitigate();
        assertEquals(Encounter.Status.MITIGATED, encounter.getStatus());

        encounter.resolve();
        assertEquals(Encounter.Status.RESOLVED, encounter.getStatus());
    }


    @Test
    void shouldRejectInvalidLifecycleTransition() {
        Encounter encounter = createEncounter();

        // Cannot mitigate before assessment.
        assertThrows(IllegalStateException.class, encounter::mitigate);

        // Natural resolution is allowed, even before assessment.
        encounter.resolve();
        assertEquals(Encounter.Status.RESOLVED, encounter.getStatus());

        // A resolved encounter cannot be resolved again.
        assertThrows(IllegalStateException.class, encounter::resolve);

        // A resolved encounter cannot be mitigated.
        assertThrows(IllegalStateException.class, encounter::mitigate);
    }


    @Test
    void shouldRejectUpdatingResolvedEncounter() {
        Encounter encounter = createEncounter();

        encounter.assess();
        encounter.resolve();

        assertThrows(
                IllegalStateException.class,
                () -> encounter.updateMeasurements(10.0, 50.0, 2.0)
        );
    }

    @Test
    void shouldRejectSameObjectTwice() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Encounter(
                        "ENC-02", "RUN-01",
                        "SAT-01", "SAT-01",
                        20.0, 100.0, 2.0, 5
                )
        );
    }

    @Test
    void shouldRejectNegativeSeparation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Encounter(
                        "ENC-02", "RUN-01",
                        "SAT-01", "DEB-01",
                        -1.0, 100.0, 2.0, 5
                )
        );
    }
}
