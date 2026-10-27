package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void shouldClampClosestApproachTimeToHorizonWhenBeyond3600Seconds() {
        Vector3D positionA = new Vector3D(0, 0, 0);
        Vector3D velocityA = new Vector3D(0, 0, 0);
        Vector3D positionB = new Vector3D(5000, 0, 0);
        Vector3D velocityB = new Vector3D(-1, 0, 0);

        // Relative position = (5000, 0, 0), relative velocity = (-1, 0, 0)
        // Unconstrained t* = -(5000 * -1) / 1^2 = 5000.0 seconds
        // Expected clamped time = 3600.0 seconds (SimulationConstants.SIMULATION_HORIZON_SECONDS)
        // Expected position of B at t=3600: 5000 - 1 * 3600 = 1400.0 km
        // Expected separation at t=3600: 1400.0 km
        // Expected relative speed: 1.0 km/s
        ClosestApproach result = new ClosestApproach(
                positionA, velocityA, positionB, velocityB
        );

        assertEquals(3600.0, result.getTimeSeconds(), 0.000001);
        assertEquals(1400.0, result.getDistanceKm(), 0.000001);

        double relativeSpeed = velocityB.subtract(velocityA).magnitude();
        assertEquals(1.0, relativeSpeed, 0.000001);
    }

    @Test
    void shouldHandleObjectsWithIdenticalInitialPositionsAndZeroRelativeVelocity() {
        // Distinct object IDs with identical initial positions (0, 0, 0) and identical velocities
        Satellite sat = new Satellite(
                "SAT-01",
                "Test Satellite",
                new Vector3D(0.0, 0.0, 0.0),
                new Vector3D(1.0, -2.0, 0.5),
                1.0,
                SpaceObject.Status.ACTIVE,
                null
        );
        Debris deb = new Debris(
                "DEB-01",
                new Vector3D(0.0, 0.0, 0.0),
                new Vector3D(1.0, -2.0, 0.5),
                1.0,
                SpaceObject.Status.ACTIVE
        );

        ClosestApproach result = new ClosestApproach(
                sat.getPosition(),
                sat.getVelocity(),
                deb.getPosition(),
                deb.getVelocity()
        );

        assertFalse(Double.isNaN(result.getTimeSeconds()));
        assertFalse(Double.isInfinite(result.getTimeSeconds()));
        assertFalse(Double.isNaN(result.getDistanceKm()));
        assertFalse(Double.isInfinite(result.getDistanceKm()));

        // Under zero relative velocity, time is 0.0 and distance is 0.0
        assertEquals(0.0, result.getTimeSeconds(), 0.000001);
        assertEquals(0.0, result.getDistanceKm(), 0.000001);

        // Also verify both objects at (0, 0, 0) with zero velocity vectors
        ClosestApproach zeroVelocityResult = new ClosestApproach(
                new Vector3D(0.0, 0.0, 0.0),
                new Vector3D(0.0, 0.0, 0.0),
                new Vector3D(0.0, 0.0, 0.0),
                new Vector3D(0.0, 0.0, 0.0)
        );

        assertFalse(Double.isNaN(zeroVelocityResult.getTimeSeconds()));
        assertFalse(Double.isInfinite(zeroVelocityResult.getTimeSeconds()));
        assertFalse(Double.isNaN(zeroVelocityResult.getDistanceKm()));
        assertFalse(Double.isInfinite(zeroVelocityResult.getDistanceKm()));

        assertEquals(0.0, zeroVelocityResult.getTimeSeconds(), 0.000001);
        assertEquals(0.0, zeroVelocityResult.getDistanceKm(), 0.000001);
    }
}
