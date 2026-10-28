package backend.model;

public class FinalObjectState {

    private String runId;
    private String objectId;

    private double posX;
    private double posY;
    private double posZ;

    private double velX;
    private double velY;
    private double velZ;

    private String status;

    public FinalObjectState() {
    }

    public FinalObjectState(
            String runId,
            String objectId,
            double posX,
            double posY,
            double posZ,
            double velX,
            double velY,
            double velZ,
            String status) {

        this.runId = runId;
        this.objectId = objectId;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.velX = velX;
        this.velY = velY;
        this.velZ = velZ;
        this.status = status;
    }

    public String getRunId() {
        return runId;
    }

    public String getObjectId() {
        return objectId;
    }

    public double getPosX() {
        return posX;
    }

    public double getPosY() {
        return posY;
    }

    public double getPosZ() {
        return posZ;
    }

    public double getVelX() {
        return velX;
    }

    public double getVelY() {
        return velY;
    }

    public double getVelZ() {
        return velZ;
    }

    public String getStatus() {
        return status;
    }
}