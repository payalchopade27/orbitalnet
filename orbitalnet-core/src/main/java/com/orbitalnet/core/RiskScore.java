package com.orbitalnet.core;

import java.util.Locale;
import java.util.Objects;

/**
 * Immutable domain model representing an explainable risk score for an encounter.
 *
 * <p>Risk factors and weights:
 * <ul>
 *   <li>Separation factor (50% weight): closer is riskier, normalized over 100.0 km.</li>
 *   <li>Velocity factor (30% weight): faster is riskier, normalized over 15.0 km/s.</li>
 *   <li>Time factor (20% weight): sooner is riskier, normalized over 3600.0 s horizon.</li>
 * </ul>
 *
 * <p>Total score formula:
 * {@code total = round(100 * (0.50 * separationFactor + 0.30 * velocityFactor + 0.20 * timeFactor))}
 */
public final class RiskScore {

    public static final double DETECTION_DISTANCE_KM = EncounterDetectionService.DETECTION_DISTANCE_KM;
    public static final double VELOCITY_NORMALIZATION_KM_S = 15.0;
    public static final double PREDICTION_HORIZON_SECONDS = SimulationConstants.SIMULATION_HORIZON_SECONDS;

    public static final double WEIGHT_SEPARATION = 0.50;
    public static final double WEIGHT_VELOCITY = 0.30;
    public static final double WEIGHT_TIME = 0.20;

    private final int totalScore;
    private final double separationFactor;
    private final double velocityFactor;
    private final double timeFactor;
    private final RiskLabel label;
    private final String explanation;

    public RiskScore(
            int totalScore,
            double separationFactor,
            double velocityFactor,
            double timeFactor,
            RiskLabel label,
            String explanation) {

        if (totalScore < 0 || totalScore > 100) {
            throw new IllegalArgumentException(
                    "Total score must be between 0 and 100, got: " + totalScore);
        }

        validateFactor(separationFactor, "Separation factor");
        validateFactor(velocityFactor, "Velocity factor");
        validateFactor(timeFactor, "Time factor");

        this.totalScore = totalScore;
        this.separationFactor = separationFactor;
        this.velocityFactor = velocityFactor;
        this.timeFactor = timeFactor;
        this.label = Objects.requireNonNull(label, "Risk label cannot be null.");

        if (explanation == null || explanation.isBlank()) {
            throw new IllegalArgumentException("Explanation cannot be blank.");
        }
        this.explanation = explanation;
    }

    private static void validateFactor(double factor, String name) {
        if (!Double.isFinite(factor) || factor < 0.0 || factor > 1.0) {
            throw new IllegalArgumentException(
                    name + " must be finite and between 0.0 and 1.0, got: " + factor);
        }
    }

    /**
     * Shared clamp helper for normalized values: max(0.0, min(1.0, value)).
     * Values below zero become zero; values above one become one.
     */
    public static double clamp(double value) {
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException("Clamped value cannot be NaN.");
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    public static double computeSeparationFactor(double minSeparationKm) {
        return clamp(1.0 - minSeparationKm / DETECTION_DISTANCE_KM);
    }

    public static double computeVelocityFactor(double relativeVelocityKmPerSec) {
        return clamp(relativeVelocityKmPerSec / VELOCITY_NORMALIZATION_KM_S);
    }

    public static double computeTimeFactor(double timeToClosestApproachSeconds) {
        return 1.0 - clamp(timeToClosestApproachSeconds / PREDICTION_HORIZON_SECONDS);
    }

    public static int computeTotalScore(
            double separationFactor,
            double velocityFactor,
            double timeFactor) {

        double weightedSum =
                WEIGHT_SEPARATION * clamp(separationFactor)
                + WEIGHT_VELOCITY * clamp(velocityFactor)
                + WEIGHT_TIME * clamp(timeFactor);

        return (int) Math.round(100.0 * weightedSum);
    }

    public static RiskLabel computeLabel(int totalScore) {
        if (totalScore < 0 || totalScore > 100) {
            throw new IllegalArgumentException(
                    "Total score must be between 0 and 100, got: " + totalScore);
        }
        if (totalScore <= 24) {
            return RiskLabel.LOW;
        }
        if (totalScore <= 49) {
            return RiskLabel.MODERATE;
        }
        if (totalScore <= 74) {
            return RiskLabel.HIGH;
        }
        return RiskLabel.CRITICAL;
    }

    public static String formatExplanation(
            int totalScore,
            double separationFactor,
            double velocityFactor,
            double timeFactor) {

        return String.format(
                Locale.ROOT,
                "Risk %d/100: separation factor %.2f, velocity factor %.2f, time factor %.2f. Weights: separation 50%%, velocity 30%%, time 20%%.",
                totalScore,
                separationFactor,
                velocityFactor,
                timeFactor
        );
    }

    public static RiskScore calculate(
            double minSeparationKm,
            double relativeVelocityKmPerSec,
            double timeToClosestApproachSeconds) {

        double sepFactor = computeSeparationFactor(minSeparationKm);
        double velFactor = computeVelocityFactor(relativeVelocityKmPerSec);
        double timeFactor = computeTimeFactor(timeToClosestApproachSeconds);

        int total = computeTotalScore(sepFactor, velFactor, timeFactor);
        RiskLabel label = computeLabel(total);
        String explanation = formatExplanation(total, sepFactor, velFactor, timeFactor);

        return new RiskScore(total, sepFactor, velFactor, timeFactor, label, explanation);
    }

    public int getTotalScore() {
        return totalScore;
    }

    public double getSeparationFactor() {
        return separationFactor;
    }

    public double getVelocityFactor() {
        return velocityFactor;
    }

    public double getTimeFactor() {
        return timeFactor;
    }

    public RiskLabel getLabel() {
        return label;
    }

    public String getExplanation() {
        return explanation;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RiskScore)) {
            return false;
        }
        RiskScore that = (RiskScore) other;
        return totalScore == that.totalScore
                && Double.compare(separationFactor, that.separationFactor) == 0
                && Double.compare(velocityFactor, that.velocityFactor) == 0
                && Double.compare(timeFactor, that.timeFactor) == 0
                && label == that.label
                && explanation.equals(that.explanation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalScore, separationFactor, velocityFactor, timeFactor, label, explanation);
    }

    @Override
    public String toString() {
        return explanation;
    }
}

