
package com.orbitalnet.core;

import java.util.Objects;

public final class ClosestApproach {

    private final double timeSeconds;
    private final double distanceKm;

    public ClosestApproach(
            Vector3D positionA,
            Vector3D velocityA,
            Vector3D positionB,
            Vector3D velocityB) {

        Objects.requireNonNull(positionA, "Position A cannot be null.");
        Objects.requireNonNull(velocityA, "Velocity A cannot be null.");
        Objects.requireNonNull(positionB, "Position B cannot be null.");
        Objects.requireNonNull(velocityB, "Velocity B cannot be null.");

        Vector3D relativePosition = positionB.subtract(positionA);
        Vector3D relativeVelocity = velocityB.subtract(velocityA);

        double velocitySquared = relativeVelocity.dot(relativeVelocity);

        double time;

        if (velocitySquared == 0.0) {
            time = 0.0;
        } else {
            time = -relativePosition.dot(relativeVelocity)
                    / velocitySquared;

            time = Math.max(0.0, Math.min(
                    time,
                    SimulationConstants.SIMULATION_HORIZON_SECONDS));
        }

        Vector3D futurePositionA = positionA.add(
                velocityA.scale(time));

        Vector3D futurePositionB = positionB.add(
                velocityB.scale(time));

        this.timeSeconds = time;
        this.distanceKm = futurePositionA.distanceTo(futurePositionB);
    }

    public double getTimeSeconds() {
        return timeSeconds;
    }

    public double getDistanceKm() {
        return distanceKm;
    }
}
