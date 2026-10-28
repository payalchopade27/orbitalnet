package backend.model;

public class RiskAssessment {

    private String encounterId;
    private int totalScore;
    private double separationFactor;
    private double velocityFactor;
    private double timeFactor;
    private String explanation;

    public RiskAssessment() {
    }

    public RiskAssessment(
            String encounterId,
            int totalScore,
            double separationFactor,
            double velocityFactor,
            double timeFactor,
            String explanation) {

        this.encounterId = encounterId;
        this.totalScore = totalScore;
        this.separationFactor = separationFactor;
        this.velocityFactor = velocityFactor;
        this.timeFactor = timeFactor;
        this.explanation = explanation;
    }

    public String getEncounterId() {
        return encounterId;
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

    public String getExplanation() {
        return explanation;
    }
}