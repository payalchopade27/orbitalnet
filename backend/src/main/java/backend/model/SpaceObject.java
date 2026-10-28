package backend.model;

public class SpaceObject {

    private String objectId;
    private String scenarioId;
    private String objectType;
    private String name;
    private String sourceType;
    private double size;
    private String status;

    private double posX;
    private double posY;
    private double posZ;

    private double velX;
    private double velY;
    private double velZ;

    private Double altitudeKm;
    private Double inclinationDeg;
    private Double periodSeconds;

    public SpaceObject() {
    }

    public SpaceObject(
            String objectId,
            String scenarioId,
            String objectType,
            String name,
            String sourceType,
            double size,
            String status,
            double posX,
            double posY,
            double posZ,
            double velX,
            double velY,
            double velZ,
            Double altitudeKm,
            Double inclinationDeg,
            Double periodSeconds) {

        this.objectId = objectId;
        this.scenarioId = scenarioId;
        this.objectType = objectType;
        this.name = name;
        this.sourceType = sourceType;
        this.size = size;
        this.status = status;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.velX = velX;
        this.velY = velY;
        this.velZ = velZ;
        this.altitudeKm = altitudeKm;
        this.inclinationDeg = inclinationDeg;
        this.periodSeconds = periodSeconds;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public String getObjectType() {
        return objectType;
    }

    public String getName() {
        return name;
    }

    public String getSourceType() {
        return sourceType;
    }

    public double getSize() {
        return size;
    }

    public String getStatus() {
        return status;
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

    public Double getAltitudeKm() {
        return altitudeKm;
    }

    public Double getInclinationDeg() {
        return inclinationDeg;
    }

    public Double getPeriodSeconds() {
        return periodSeconds;
    }
}