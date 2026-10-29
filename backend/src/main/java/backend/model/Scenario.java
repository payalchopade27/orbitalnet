package backend.model;

public class Scenario {

    private String scenarioId;
    private String name;
    private long seed;
    private String generationConfig;
    private String status;
    private String createdAt;

    public Scenario() {
    }

    public Scenario(
            String scenarioId,
            String name,
            long seed,
            String generationConfig,
            String status,
            String createdAt) {

        this.scenarioId = scenarioId;
        this.name = name;
        this.seed = seed;
        this.generationConfig = generationConfig;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public String getName() {
        return name;
    }

    public long getSeed() {
        return seed;
    }

    public String getGenerationConfig() {
        return generationConfig;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}