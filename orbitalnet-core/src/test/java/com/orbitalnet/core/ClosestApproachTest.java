
package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClosestApproachTest {

    @Test
    void shouldFindClosestApproachWhenObjectsMoveTowardsEachOther() {
        ClosestApproach result = new ClosestApproach(
                new Vector3D(-100, 0, 0),
                new Vector3D(1, 0, 0),
                new Vector3D(100, 0, 0),
                new Vector3D(-1, 0, 0)
        );

        assertEquals(100.0, result.getTimeSeconds(), 0.000001);
        assertEquals(0.0, result.getDistanceKm(), 0.000001);
    }

    @Test
    void shouldUseTimeZeroWhenObjectsAreMovingApart() {
        ClosestApproach result = new ClosestApproach(
                new Vector3D(0, 0, 0),
                new Vector3D(-1, 0, 0),
                new Vector3D(10, 0, 0),
                new Vector3D(1, 0, 0)
        );

        assertEquals(0.0, result.getTimeSeconds(), 0.000001);
        assertEquals(10.0, result.getDistanceKm(), 0.000001);
    }

    @Test
    void shouldHandleObjectsWithIdenticalVelocities() {
        ClosestApproach result = new ClosestApproach(
                new Vector3D(0, 0, 0),
                new Vector3D(1, 0, 0),
                new Vector3D(3, 4, 0),
                new Vector3D(1, 0, 0)
        );

        assertEquals(0.0, result.getTimeSeconds(), 0.000001);
        assertEquals(5.0, result.getDistanceKm(), 0.000001);
    }
}
