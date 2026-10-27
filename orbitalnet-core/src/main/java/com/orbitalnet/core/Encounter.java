package com.orbitalnet.core;

import java.util.Objects;

public final class Encounter {

    public enum Status {
        DETECTED,
        ASSESSED,
        MITIGATED,
        RESOLVED
    }

    private final String encounterId;
    private final String runId;
    private final String objectAId;
    private final String objectBId;
    private final int detectedAtTick;

    private double minSeparationKm;
    private double timeToClosestApproachSeconds;
    private double relativeVelocityKmPerSec;
    private Status status;
    private RiskScore riskScore;

    public Encounter(
            String encounterId,
            String runId,
            String objectAId,
            String objectBId,
            double minSeparationKm,
            double timeToClosestApproachSeconds,
            double relativeVelocityKmPerSec,
            int detectedAtTick) {

        if (encounterId == null || encounterId.isBlank()) {
            throw new IllegalArgumentException("Encounter ID cannot be blank.");
        }

        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("Run ID cannot be blank.");
        }

        if (objectAId == null || objectAId.isBlank()
                || objectBId == null || objectBId.isBlank()) {
            throw new IllegalArgumentException("Object IDs cannot be blank.");
        }

        if (objectAId.equals(objectBId)) {
            throw new IllegalArgumentException(
                    "An encounter must involve two different objects.");
        }

        validateMeasurements(
                minSeparationKm,
                timeToClosestApproachSeconds,
                relativeVelocityKmPerSec);

        if (detectedAtTick < 0) {
            throw new IllegalArgumentException("Detection tick cannot be negative.");
        }

        this.encounterId = encounterId;
        this.runId = runId;

        // Store object IDs in canonical (consistent) order.
        if (objectAId.compareTo(objectBId) < 0) {
            this.objectAId = objectAId;
            this.objectBId = objectBId;
        } else {
            this.objectAId = objectBId;
            this.objectBId = objectAId;
        }

        this.minSeparationKm = minSeparationKm;
        this.timeToClosestApproachSeconds = timeToClosestApproachSeconds;
        this.relativeVelocityKmPerSec = relativeVelocityKmPerSec;
        this.detectedAtTick = detectedAtTick;
        this.status = Status.DETECTED;
        this.riskScore = null;
    }

    private void validateMeasurements(
            double separation,
            double time,
            double relativeVelocity) {

        if (!Double.isFinite(separation) || separation < 0.0) {
            throw new IllegalArgumentException(
                    "Minimum separation must be finite and non-negative.");
        }

        if (!Double.isFinite(time)
                || time < 0.0
                || time > SimulationConstants.SIMULATION_HORIZON_SECONDS) {
            throw new IllegalArgumentException(
                    "Time to closest approach must be between 0 and 3600 seconds.");
        }

        if (!Double.isFinite(relativeVelocity) || relativeVelocity < 0.0) {
            throw new IllegalArgumentException(
                    "Relative velocity must be finite and non-negative.");
        }
    }

    public String getEncounterId() {
        return encounterId;
    }

    public String getRunId() {
        return runId;
    }

    public String getObjectAId() {
        return objectAId;
    }

    public String getObjectBId() {
        return objectBId;
    }

    public double getMinSeparationKm() {
        return minSeparationKm;
    }

    public double getTimeToClosestApproachSeconds() {
        return timeToClosestApproachSeconds;
    }

    public double getRelativeVelocityKmPerSec() {
        return relativeVelocityKmPerSec;
    }

    public int getDetectedAtTick() {
        return detectedAtTick;
    }

    public Status getStatus() {
        return status;
    }

    public RiskScore getRiskScore() {
        return riskScore;
    }

    public RiskScore getLatestRiskScore() {
        return riskScore;
    }

    public void updateMeasurements(
            double separationKm,
            double timeToClosestApproachSeconds,
            double relativeVelocityKmPerSec) {

        if (status == Status.RESOLVED) {
            throw new IllegalStateException(
                    "A resolved encounter cannot be updated.");
        }

        validateMeasurements(
                separationKm,
                timeToClosestApproachSeconds,
                relativeVelocityKmPerSec);

        this.minSeparationKm = separationKm;
        this.timeToClosestApproachSeconds = timeToClosestApproachSeconds;
        this.relativeVelocityKmPerSec = relativeVelocityKmPerSec;

        if (this.riskScore != null) {
            this.riskScore = RiskScore.calculate(
                    separationKm,
                    relativeVelocityKmPerSec,
                    timeToClosestApproachSeconds);
        }
    }

    public void updateRiskScore(RiskScore riskScore) {
        if (status == Status.RESOLVED) {
            throw new IllegalStateException(
                    "A resolved encounter cannot be updated.");
        }

        this.riskScore = Objects.requireNonNull(
                riskScore, "Risk score cannot be null.");
    }

    public void assess() {
        if (status != Status.DETECTED) {
            throw new IllegalStateException(
                    "Only a detected encounter can be assessed.");
        }

        status = Status.ASSESSED;
        if (this.riskScore == null) {
            this.riskScore = RiskScore.calculate(
                    minSeparationKm,
                    relativeVelocityKmPerSec,
                    timeToClosestApproachSeconds);
        }
    }

    public void assess(RiskScore riskScore) {
        if (status != Status.DETECTED) {
            throw new IllegalStateException(
                    "Only a detected encounter can be assessed.");
        }

        status = Status.ASSESSED;
        this.riskScore = Objects.requireNonNull(
                riskScore, "Risk score cannot be null.");
    }

    public void mitigate() {
        if (status != Status.ASSESSED) {
            throw new IllegalStateException(
                    "Only an assessed encounter can be mitigated.");
        }

        status = Status.MITIGATED;
    }

    public void resolve() {
        if (status == Status.RESOLVED) {
            throw new IllegalStateException(
                    "Encounter is already resolved.");
        }

        status = Status.RESOLVED;
    }

}
