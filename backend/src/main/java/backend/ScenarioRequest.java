
package backend;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class ScenarioRequest {

    @Min(1)
    @Max(100)
    private int satellites;

    @Min(0)
    @Max(500)
    private int debris;

    @Min(1)
    @Max(1440)
    private int duration;

    public int getSatellites() {
        return satellites;
    }

    public void setSatellites(int satellites) {
        this.satellites = satellites;
    }

    public int getDebris() {
        return debris;
    }

    public void setDebris(int debris) {
        this.debris = debris;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
