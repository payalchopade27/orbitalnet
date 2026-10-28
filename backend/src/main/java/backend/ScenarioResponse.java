
package backend;

public class ScenarioResponse {
    private String scenarioId;
    private String message;
    private String status;
    private int satellites;
    private int debris;
    private int duration;

    public ScenarioResponse(
            String scenarioId,
            String message,
            String status,
            int satellites,
            int debris,
            int duration) {
        this.scenarioId = scenarioId;
        this.message = message;
        this.status = status;
        this.satellites = satellites;
        this.debris = debris;
        this.duration = duration;
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }

    public int getSatellites() {
        return satellites;
    }

    public int getDebris() {
        return debris;
    }

    public int getDuration() {
        return duration;
    }
}
