package backend.model;

public class SimulationRun {

    private String runId;
    private String scenarioId;
    private String status;
    private long currentTick;
    private double elapsedTimeSeconds;
    private String algorithmUsed;
    private String baselineRunId;
    private String maneuverCandidateId;

    public SimulationRun() {
    }

    public SimulationRun(
            String runId,
            String scenarioId,
            String status,
            long currentTick,
            double elapsedTimeSeconds,
            String algorithmUsed,
            String baselineRunId,
            String maneuverCandidateId) {

        this.runId = runId;
        this.scenarioId = scenarioId;
        this.status = status;
        this.currentTick = currentTick;
        this.elapsedTimeSeconds = elapsedTimeSeconds;
        this.algorithmUsed = algorithmUsed;
        this.baselineRunId = baselineRunId;
        this.maneuverCandidateId = maneuverCandidateId;
    }

    public String getRunId() {
        return runId;
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public String getStatus() {
        return status;
    }

    public long getCurrentTick() {
        return currentTick;
    }

    public double getElapsedTimeSeconds() {
        return elapsedTimeSeconds;
    }

    public String getAlgorithmUsed() {
        return algorithmUsed;
    }

    public String getBaselineRunId() {
        return baselineRunId;
    }

    public String getManeuverCandidateId() {
        return maneuverCandidateId;
    }
}