
package com.orbitalnet.core;

public final class ScenarioGenerationConfig {

    private final int satelliteCount;
    private final int debrisCount;

    private final double minPositionKm;
    private final double maxPositionKm;

    private final double minVelocityKmPerSec;
    private final double maxVelocityKmPerSec;

    private final double minSizeKm;
    private final double maxSizeKm;

    public ScenarioGenerationConfig(
            int satelliteCount,
            int debrisCount,
            double minPositionKm,
            double maxPositionKm,
            double minVelocityKmPerSec,
            double maxVelocityKmPerSec,
            double minSizeKm,
            double maxSizeKm) {

        if (satelliteCount < 0 || debrisCount < 0) {
            throw new IllegalArgumentException(
                    "Object counts cannot be negative.");
        }

        validateRange(minPositionKm, maxPositionKm, "position");
        validateRange(minVelocityKmPerSec, maxVelocityKmPerSec, "velocity");
        validateRange(minSizeKm, maxSizeKm, "size");

        if (minSizeKm <= 0) {
            throw new IllegalArgumentException(
                    "Object size must be greater than zero.");
        }

        this.satelliteCount = satelliteCount;
        this.debrisCount = debrisCount;

        this.minPositionKm = minPositionKm;
        this.maxPositionKm = maxPositionKm;

        this.minVelocityKmPerSec = minVelocityKmPerSec;
        this.maxVelocityKmPerSec = maxVelocityKmPerSec;

        this.minSizeKm = minSizeKm;
        this.maxSizeKm = maxSizeKm;
    }

    private static void validateRange(
            double minimum, double maximum, String fieldName) {

        if (!Double.isFinite(minimum)
                || !Double.isFinite(maximum)
                || minimum > maximum) {

            throw new IllegalArgumentException(
                    "Invalid " + fieldName + " range.");
        }
    }

    public int getSatelliteCount() {
        return satelliteCount;
    }

    public int getDebrisCount() {
        return debrisCount;
    }

    public double getMinPositionKm() {
        return minPositionKm;
    }

    public double getMaxPositionKm() {
        return maxPositionKm;
    }

    public double getMinVelocityKmPerSec() {
        return minVelocityKmPerSec;
    }

    public double getMaxVelocityKmPerSec() {
        return maxVelocityKmPerSec;
    }

    public double getMinSizeKm() {
        return minSizeKm;
    }

    public double getMaxSizeKm() {
        return maxSizeKm;
    }
}
