package backend.model;

public class ManeuverCandidate {

    private String maneuverCandidateId;
    private String encounterId;
    private String targetObjectId;
    private double deltaVX;
    private double deltaVY;
    private double deltaVZ;
    private double newMinSeparationKm;
    private double cost;
    private int rank;

    public ManeuverCandidate() {
    }

    public ManeuverCandidate(
            String maneuverCandidateId,
            String encounterId,
            String targetObjectId,
            double deltaVX,
            double deltaVY,
            double deltaVZ,
            double newMinSeparationKm,
            double cost,
            int rank) {

        this.maneuverCandidateId = maneuverCandidateId;
        this.encounterId = encounterId;
        this.targetObjectId = targetObjectId;
        this.deltaVX = deltaVX;
        this.deltaVY = deltaVY;
        this.deltaVZ = deltaVZ;
        this.newMinSeparationKm = newMinSeparationKm;
        this.cost = cost;
        this.rank = rank;
    }

    public String getManeuverCandidateId() {
        return maneuverCandidateId;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public String getTargetObjectId() {
        return targetObjectId;
    }

    public double getDeltaVX() {
        return deltaVX;
    }

    public double getDeltaVY() {
        return deltaVY;
    }

    public double getDeltaVZ() {
        return deltaVZ;
    }

    public double getNewMinSeparationKm() {
        return newMinSeparationKm;
    }

    public double getCost() {
        return cost;
    }

    public int getRank() {
        return rank;
    }
}