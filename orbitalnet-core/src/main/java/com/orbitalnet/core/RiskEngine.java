package com.orbitalnet.core;

import java.util.Objects;

/**
 * Service engine evaluating explainable risk for encounters.
 */
public final class RiskEngine {

    public RiskScore assess(
            double minSeparationKm,
            double relativeVelocityKmPerSec,
            double timeToClosestApproachSeconds) {

        return RiskScore.calculate(
                minSeparationKm,
                relativeVelocityKmPerSec,
                timeToClosestApproachSeconds);
    }

    public RiskScore assess(Encounter encounter) {
        Objects.requireNonNull(encounter, "Encounter cannot be null.");
        return assess(
                encounter.getMinSeparationKm(),
                encounter.getRelativeVelocityKmPerSec(),
                encounter.getTimeToClosestApproachSeconds()
        );
    }
}

