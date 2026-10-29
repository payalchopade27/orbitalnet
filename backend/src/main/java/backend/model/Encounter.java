package backend.model;

public class Encounter {

    private String encounterId;
    private String runId;
    private String objectAId;
    private String objectBId;
    private double minSeparationKm;
    private double timeToClosestApproachSeconds;
    private double relativeVelocityKmPerSec;
    private long detectedAtTick;
    private String status;

    public Encounter() {
    }

    public Encounter(
            String encounterId,
            String runId,
            String objectAId,
            String objectBId,
            double minSeparationKm,
            double timeToClosestApproachSeconds,
            double relativeVelocityKmPerSec,
            long detectedAtTick,
            String status) {

        this.encounterId = encounterId;
        this.runId = runId;
        this.objectAId = objectAId;
        this.objectBId = objectBId;
        this.minSeparationKm = minSeparationKm;
        this.timeToClosestApproachSeconds = timeToClosestApproachSeconds;
        this.relativeVelocityKmPerSec = relativeVelocityKmPerSec;
        this.detectedAtTick = detectedAtTick;
        this.status = status;
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

    public long getDetectedAtTick() {
        return detectedAtTick;
    }

    public String getStatus() {
        return status;
    }
}