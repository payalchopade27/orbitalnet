package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RiskScoreTest {

    @Test
    void clampBehavesCorrectlyAcrossRanges() {
        assertEquals(0.0, RiskScore.clamp(-1.0), 0.000001);
        assertEquals(0.0, RiskScore.clamp(-0.00001), 0.000001);
        assertEquals(0.0, RiskScore.clamp(0.0), 0.000001);
        assertEquals(0.42, RiskScore.clamp(0.42), 0.000001);
        assertEquals(1.0, RiskScore.clamp(1.0), 0.000001);
        assertEquals(1.0, RiskScore.clamp(1.0001), 0.000001);
        assertEquals(1.0, RiskScore.clamp(100.0), 0.000001);

        assertThrows(IllegalArgumentException.class, () -> RiskScore.clamp(Double.NaN));
    }

    @Test
    void separationFactorCalculatesAndClampsCorrectly() {
        // Zero separation -> maximum risk (factor 1.0)
        assertEquals(1.0, RiskScore.computeSeparationFactor(0.0), 0.000001);

        // Mid separation 50 km -> factor 0.50
        assertEquals(0.50, RiskScore.computeSeparationFactor(50.0), 0.000001);

        // Threshold separation 100 km -> factor 0.0
        assertEquals(0.0, RiskScore.computeSeparationFactor(100.0), 0.000001);

        // Beyond threshold (out-of-range positive) -> clamped to 0.0
        assertEquals(0.0, RiskScore.computeSeparationFactor(150.0), 0.000001);
        assertEquals(0.0, RiskScore.computeSeparationFactor(1000.0), 0.000001);

        // Negative separation (out-of-range negative) -> clamped to 1.0
        assertEquals(1.0, RiskScore.computeSeparationFactor(-20.0), 0.000001);
    }

    @Test
    void velocityFactorCalculatesAndClampsCorrectly() {
        // Zero relative velocity -> minimum risk (factor 0.0)
        assertEquals(0.0, RiskScore.computeVelocityFactor(0.0), 0.000001);

        // Half normalization 7.5 km/s -> factor 0.50
        assertEquals(0.50, RiskScore.computeVelocityFactor(7.5), 0.000001);

        // Normalization threshold 15.0 km/s -> factor 1.0
        assertEquals(1.0, RiskScore.computeVelocityFactor(15.0), 0.000001);

        // Above threshold 30.0 km/s -> clamped to 1.0
        assertEquals(1.0, RiskScore.computeVelocityFactor(30.0), 0.000001);
        assertEquals(1.0, RiskScore.computeVelocityFactor(100.0), 0.000001);

        // Negative velocity -> clamped to 0.0
        assertEquals(0.0, RiskScore.computeVelocityFactor(-5.0), 0.000001);
    }

    @Test
    void timeFactorCalculatesAndClampsCorrectly() {
        // Immediate closest approach (time 0) -> maximum risk (factor 1.0)
        assertEquals(1.0, RiskScore.computeTimeFactor(0.0), 0.000001);

        // Mid horizon (1800 s) -> factor 0.50
        assertEquals(0.50, RiskScore.computeTimeFactor(1800.0), 0.000001);

        // Horizon limit (3600 s) -> factor 0.0
        assertEquals(0.0, RiskScore.computeTimeFactor(3600.0), 0.000001);

        // Beyond horizon (out-of-range positive) -> clamped to 0.0
        assertEquals(0.0, RiskScore.computeTimeFactor(5000.0), 0.000001);

        // Negative time (out-of-range negative) -> clamped to 1.0
        assertEquals(1.0, RiskScore.computeTimeFactor(-100.0), 0.000001);
    }

    @Test
    void totalScoreMatchesHandCalculatedExamplesAndRounding() {
        // Extreme 1: Maximum risk across all factors
        // 100 * (0.50 * 1.0 + 0.30 * 1.0 + 0.20 * 1.0) = 100
        assertEquals(100, RiskScore.computeTotalScore(1.0, 1.0, 1.0));

        // Extreme 2: Minimum risk across all factors
        // 100 * (0.50 * 0.0 + 0.30 * 0.0 + 0.20 * 0.0) = 0
        assertEquals(0, RiskScore.computeTotalScore(0.0, 0.0, 0.0));

        // Balanced: 0.50 across all factors -> 50
        assertEquals(50, RiskScore.computeTotalScore(0.50, 0.50, 0.50));

        // Hand calculation: sep=0.25, vel=0.30, time=0.15
        // weightedSum = 0.50*0.25 + 0.30*0.30 + 0.20*0.15 = 0.125 + 0.090 + 0.030 = 0.245
        // 100 * 0.245 = 24.5 -> rounds to 25
        assertEquals(25, RiskScore.computeTotalScore(0.25, 0.30, 0.15));

        // Hand calculation: sep=0.24, vel=0.30, time=0.17
        // weightedSum = 0.50*0.24 + 0.30*0.30 + 0.20*0.17 = 0.120 + 0.090 + 0.034 = 0.244
        // 100 * 0.244 = 24.4 -> rounds down to 24
        assertEquals(24, RiskScore.computeTotalScore(0.24, 0.30, 0.17));

        // Hand calculation near 75 boundary:
        // sep=0.80, vel=0.75, time=0.60
        // weightedSum = 0.50*0.80 + 0.30*0.75 + 0.20*0.60 = 0.400 + 0.225 + 0.120 = 0.745
        // 100 * 0.745 = 74.5 -> rounds to 75
        assertEquals(75, RiskScore.computeTotalScore(0.80, 0.75, 0.60));

        // sep=0.80, vel=0.74, time=0.61
        // weightedSum = 0.50*0.80 + 0.30*0.74 + 0.20*0.61 = 0.400 + 0.222 + 0.122 = 0.744
        // 100 * 0.744 = 74.4 -> rounds down to 74
        assertEquals(74, RiskScore.computeTotalScore(0.80, 0.74, 0.61));
    }

    @Test
    void labelBoundariesMapCorrectly() {
        // LOW: 0 - 24
        assertEquals(RiskLabel.LOW, RiskScore.computeLabel(0));
        assertEquals(RiskLabel.LOW, RiskScore.computeLabel(10));
        assertEquals(RiskLabel.LOW, RiskScore.computeLabel(24));

        // MODERATE: 25 - 49
        assertEquals(RiskLabel.MODERATE, RiskScore.computeLabel(25));
        assertEquals(RiskLabel.MODERATE, RiskScore.computeLabel(37));
        assertEquals(RiskLabel.MODERATE, RiskScore.computeLabel(49));

        // HIGH: 50 - 74
        assertEquals(RiskLabel.HIGH, RiskScore.computeLabel(50));
        assertEquals(RiskLabel.HIGH, RiskScore.computeLabel(62));
        assertEquals(RiskLabel.HIGH, RiskScore.computeLabel(74));

        // CRITICAL: 75 - 100
        assertEquals(RiskLabel.CRITICAL, RiskScore.computeLabel(75));
        assertEquals(RiskLabel.CRITICAL, RiskScore.computeLabel(90));
        assertEquals(RiskLabel.CRITICAL, RiskScore.computeLabel(100));

        // Out of bounds scores rejected
        assertThrows(IllegalArgumentException.class, () -> RiskScore.computeLabel(-1));
        assertThrows(IllegalArgumentException.class, () -> RiskScore.computeLabel(101));
    }

    @Test
    void deterministicExplanationFormatting() {
        String explanation = RiskScore.formatExplanation(94, 1.0, 0.90, 0.85);
        assertEquals(
                "Risk 94/100: separation factor 1.00, velocity factor 0.90, time factor 0.85. Weights: separation 50%, velocity 30%, time 20%.",
                explanation
        );

        String zeroExplanation = RiskScore.formatExplanation(0, 0.0, 0.0, 0.0);
        assertEquals(
                "Risk 0/100: separation factor 0.00, velocity factor 0.00, time factor 0.00. Weights: separation 50%, velocity 30%, time 20%.",
                zeroExplanation
        );

        // Identical inputs produce identical strings
        String exp1 = RiskScore.formatExplanation(50, 0.50, 0.50, 0.50);
        String exp2 = RiskScore.formatExplanation(50, 0.50, 0.50, 0.50);
        assertEquals(exp1, exp2);
    }

    @Test
    void calculateProducesCompleteValidRiskScore() {
        // minSeparation: 20 km -> sepFactor = 1 - 20/100 = 0.80
        // relVelocity: 7.5 km/s -> velFactor = 7.5/15 = 0.50
        // timeToCA: 900 s -> timeFactor = 1 - 900/3600 = 0.75
        // weightedSum = 0.50*0.80 + 0.30*0.50 + 0.20*0.75 = 0.40 + 0.15 + 0.15 = 0.70
        // total = 70 -> HIGH
        RiskScore score = RiskScore.calculate(20.0, 7.5, 900.0);

        assertEquals(70, score.getTotalScore());
        assertEquals(0.80, score.getSeparationFactor(), 0.000001);
        assertEquals(0.50, score.getVelocityFactor(), 0.000001);
        assertEquals(0.75, score.getTimeFactor(), 0.000001);
        assertEquals(RiskLabel.HIGH, score.getLabel());
        assertEquals(
                "Risk 70/100: separation factor 0.80, velocity factor 0.50, time factor 0.75. Weights: separation 50%, velocity 30%, time 20%.",
                score.getExplanation()
        );
        assertEquals(score.getExplanation(), score.toString());
    }

    @Test
    void riskEngineAssessesDirectlyAndFromEncounter() {
        RiskEngine engine = new RiskEngine();

        RiskScore scoreDirect = engine.assess(20.0, 7.5, 900.0);
        assertEquals(70, scoreDirect.getTotalScore());

        Encounter encounter = new Encounter(
                "ENC-01", "RUN-01", "SAT-01", "DEB-01",
                20.0, 900.0, 7.5, 10
        );

        RiskScore scoreFromEncounter = engine.assess(encounter);
        assertEquals(scoreDirect, scoreFromEncounter);
    }

    @Test
    void encounterIntegrationAndReassessmentUpdatesLatestScore() {
        Encounter encounter = new Encounter(
                "ENC-01", "RUN-01", "SAT-01", "DEB-01",
                50.0, 1800.0, 7.5, 5
        );

        // Before assessment, risk score is null
        assertNull(encounter.getRiskScore());
        assertNull(encounter.getLatestRiskScore());
        assertEquals(Encounter.Status.DETECTED, encounter.getStatus());
        assertEquals(5, encounter.getDetectedAtTick());

        // Assessing computes risk score
        // 50 km -> sep=0.50; 7.5 km/s -> vel=0.50; 1800 s -> time=0.50; total=50
        encounter.assess();
        assertEquals(Encounter.Status.ASSESSED, encounter.getStatus());
        assertNotNull(encounter.getRiskScore());
        assertEquals(50, encounter.getRiskScore().getTotalScore());
        assertEquals(RiskLabel.HIGH, encounter.getRiskScore().getLabel());
        assertEquals(5, encounter.getDetectedAtTick());

        // Updating measurements updates the latest risk score automatically
        // New measurements: 0 km -> sep=1.00; 15 km/s -> vel=1.00; 0 s -> time=1.00; total=100
        encounter.updateMeasurements(0.0, 0.0, 15.0);
        assertEquals(100, encounter.getRiskScore().getTotalScore());
        assertEquals(RiskLabel.CRITICAL, encounter.getRiskScore().getLabel());
        assertEquals(5, encounter.getDetectedAtTick()); // detectedAtTick preserved

        // Explicit updateRiskScore replaces the score
        RiskScore customScore = new RiskScore(
                10, 0.1, 0.1, 0.1, RiskLabel.LOW, "Custom explanation."
        );
        encounter.updateRiskScore(customScore);
        assertEquals(customScore, encounter.getRiskScore());
        assertEquals(10, encounter.getLatestRiskScore().getTotalScore());
        assertEquals(5, encounter.getDetectedAtTick());

        // Reassessment cannot occur on already assessed encounter
        assertThrows(IllegalStateException.class, encounter::assess);

        // Resolved encounter rejects updates
        encounter.resolve();
        assertThrows(IllegalStateException.class,
                () -> encounter.updateMeasurements(10.0, 10.0, 1.0));
        assertThrows(IllegalStateException.class,
                () -> encounter.updateRiskScore(customScore));
    }

    @Test
    void riskScoreValidatesConstructorArguments() {
        assertThrows(IllegalArgumentException.class, () ->
                new RiskScore(-1, 0.5, 0.5, 0.5, RiskLabel.LOW, "text"));
        assertThrows(IllegalArgumentException.class, () ->
                new RiskScore(101, 0.5, 0.5, 0.5, RiskLabel.CRITICAL, "text"));
        assertThrows(IllegalArgumentException.class, () ->
                new RiskScore(50, -0.1, 0.5, 0.5, RiskLabel.HIGH, "text"));
        assertThrows(IllegalArgumentException.class, () ->
                new RiskScore(50, 1.1, 0.5, 0.5, RiskLabel.HIGH, "text"));
        assertThrows(NullPointerException.class, () ->
                new RiskScore(50, 0.5, 0.5, 0.5, null, "text"));
        assertThrows(IllegalArgumentException.class, () ->
                new RiskScore(50, 0.5, 0.5, 0.5, RiskLabel.HIGH, ""));
    }
}

