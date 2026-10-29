package com.orbitalnet.core;

import java.util.Objects;

/**
 * Immutable input model capturing the baseline execution context required
 * to perform a retrospective avoidance maneuver replay in a distinct simulation run.
 *
 * <p>Preserves the baseline run identifier, scenario initial conditions, detection-time
 * kinematic measurements, and target/other object IDs without retaining mutable references
 * or modifying the original baseline encounter.
 */
public final class ManeuverReplayInput {

    private final String baselineRunId;
    private final Scenario scenario;
    private final String encounterId;
    private final int detectedAtTick;
    private final String targetObjectId;
    private final String otherObjectId;
    private final double detectionTimeMinSeparationKm;
    private final double detectionTimeTimeToClosestApproachSeconds;
    private final double detectionTimeRelativeVelocityKmPerSec;
    private final RiskScore detectionTimeRiskScore;

    public ManeuverReplayInput(
            String baselineRunId,
            Scenario scenario,
            String encounterId,
            int detectedAtTick,
            String targetObjectId,
            String otherObjectId,
            double detectionTimeMinSeparationKm,
            double detectionTimeTimeToClosestApproachSeconds,
            double detectionTimeRelativeVelocityKmPerSec,
            RiskScore detectionTimeRiskScore) {

        if (baselineRunId == null || baselineRunId.isBlank()) {
            throw new IllegalArgumentException("Baseline run ID cannot be blank.");
        }
        this.scenario = Objects.requireNonNull(scenario, "Scenario cannot be null.");
        if (encounterId == null || encounterId.isBlank()) {
            throw new IllegalArgumentException("Encounter ID cannot be blank.");
        }

        int maxTicks = (int) (SimulationConstants.SIMULATION_HORIZON_SECONDS
                / SimulationConstants.DELTA_TIME_SECONDS);
        if (detectedAtTick < 0 || detectedAtTick > maxTicks) {
            throw new IllegalArgumentException(
                    "Detection tick must be within simulation horizon [0, " + maxTicks + "], got: " + detectedAtTick);
        }

        if (targetObjectId == null || targetObjectId.isBlank()) {
            throw new IllegalArgumentException("Target object ID cannot be blank.");
        }
        if (otherObjectId == null || otherObjectId.isBlank()) {
            throw new IllegalArgumentException("Other object ID cannot be blank.");
        }
        if (targetObjectId.equals(otherObjectId)) {
            throw new IllegalArgumentException("Target and other object IDs must be different.");
        }

        validateMeasurements(
                detectionTimeMinSeparationKm,
                detectionTimeTimeToClosestApproachSeconds,
                detectionTimeRelativeVelocityKmPerSec
        );

        this.baselineRunId = baselineRunId;
        this.encounterId = encounterId;
        this.detectedAtTick = detectedAtTick;
        this.targetObjectId = targetObjectId;
        this.otherObjectId = otherObjectId;
        this.detectionTimeMinSeparationKm = detectionTimeMinSeparationKm;
        this.detectionTimeTimeToClosestApproachSeconds = detectionTimeTimeToClosestApproachSeconds;
        this.detectionTimeRelativeVelocityKmPerSec = detectionTimeRelativeVelocityKmPerSec;
        this.detectionTimeRiskScore = detectionTimeRiskScore;
    }

    private static void validateMeasurements(
            double separation,
            double time,
            double relativeVelocity) {

        if (!Double.isFinite(separation) || separation < 0.0) {
            throw new IllegalArgumentException("Detection-time minimum separation must be finite and non-negative.");
        }
        if (!Double.isFinite(time)
                || time < 0.0
                || time > SimulationConstants.SIMULATION_HORIZON_SECONDS) {
            throw new IllegalArgumentException(
                    "Detection-time time to closest approach must be between 0 and 3600 seconds.");
        }
        if (!Double.isFinite(relativeVelocity) || relativeVelocity < 0.0) {
            throw new IllegalArgumentException("Detection-time relative velocity must be finite and non-negative.");
        }
    }

    public String getBaselineRunId() {
        return baselineRunId;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public int getDetectedAtTick() {
        return detectedAtTick;
    }

    public String getTargetObjectId() {
        return targetObjectId;
    }

    public String getOtherObjectId() {
        return otherObjectId;
    }

    public double getDetectionTimeMinSeparationKm() {
        return detectionTimeMinSeparationKm;
    }

    public double getDetectionTimeTimeToClosestApproachSeconds() {
        return detectionTimeTimeToClosestApproachSeconds;
    }

    public double getDetectionTimeRelativeVelocityKmPerSec() {
        return detectionTimeRelativeVelocityKmPerSec;
    }

    public RiskScore getDetectionTimeRiskScore() {
        return detectionTimeRiskScore;
    }

    /**
     * Reconstructs an independent {@link SpaceObject} instance for the target object
     * at the exact detection tick boundary using constant-velocity propagation.
     */
    public SpaceObject reconstructTargetObjectAtDetectionTick() {
        return reconstructObjectAtDetectionTick(targetObjectId);
    }

    /**
     * Reconstructs an independent {@link SpaceObject} instance for the other object
     * in the encounter pair at the exact detection tick boundary using constant-velocity propagation.
     */
    public SpaceObject reconstructOtherObjectAtDetectionTick() {
        return reconstructObjectAtDetectionTick(otherObjectId);
    }

    /**
     * Reconstructs an independent {@link SpaceObject} instance for any scenario object
     * at the exact detection tick boundary.
     */
    public SpaceObject reconstructObjectAtDetectionTick(String objectId) {
        Objects.requireNonNull(objectId, "Object ID cannot be null.");
        for (InitialObjectState state : scenario.getInitialObjectStates()) {
            if (state.getId().equals(objectId)) {
                SpaceObject obj = state.toSpaceObject();
                if (detectedAtTick > 0 && obj.isActive()) {
                    obj.advancePosition(detectedAtTick * SimulationConstants.DELTA_TIME_SECONDS);
                }
                return obj;
            }
        }
        throw new IllegalStateException("Object '" + objectId + "' was not found in scenario initial states.");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ManeuverReplayInput that)) return false;
        return detectedAtTick == that.detectedAtTick
                && Double.compare(detectionTimeMinSeparationKm, that.detectionTimeMinSeparationKm) == 0
                && Double.compare(detectionTimeTimeToClosestApproachSeconds, that.detectionTimeTimeToClosestApproachSeconds) == 0
                && Double.compare(detectionTimeRelativeVelocityKmPerSec, that.detectionTimeRelativeVelocityKmPerSec) == 0
                && baselineRunId.equals(that.baselineRunId)
                && scenario.getScenarioId().equals(that.scenario.getScenarioId())
                && encounterId.equals(that.encounterId)
                && targetObjectId.equals(that.targetObjectId)
                && otherObjectId.equals(that.otherObjectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                baselineRunId,
                scenario.getScenarioId(),
                encounterId,
                detectedAtTick,
                targetObjectId,
                otherObjectId,
                detectionTimeMinSeparationKm,
                detectionTimeTimeToClosestApproachSeconds,
                detectionTimeRelativeVelocityKmPerSec
        );
    }

    @Override
    public String toString() {
        return "ManeuverReplayInput{" +
                "baselineRunId='" + baselineRunId + '\'' +
                ", scenarioId='" + scenario.getScenarioId() + '\'' +
                ", encounterId='" + encounterId + '\'' +
                ", detectedAtTick=" + detectedAtTick +
                ", targetObjectId='" + targetObjectId + '\'' +
                ", otherObjectId='" + otherObjectId + '\'' +
                ", detectionTimeMinSeparationKm=" + detectionTimeMinSeparationKm +
                ", detectionTimeTimeToClosestApproachSeconds=" + detectionTimeTimeToClosestApproachSeconds +
                ", detectionTimeRelativeVelocityKmPerSec=" + detectionTimeRelativeVelocityKmPerSec +
                ", detectionTimeRiskScore=" + detectionTimeRiskScore +
                '}';
    }
}

