package com.orbitalnet.core;

import java.util.Objects;

/**
 * Immutable domain model representing a candidate collision avoidance maneuver.
 *
 * <p>Contains the target object being maneuvered, the applied delta-v impulse,
 * the predicted minimum separation distance, the maneuver cost (magnitude of delta-v),
 * and the rank within the associated encounter.
 */
public final class ManeuverCandidate {

    private final String maneuverCandidateId;
    private final String encounterId;
    private final String targetObjectId;
    private final Vector3D deltaV;
    private final double newMinSeparationKm;
    private final double cost;
    private final int rank;

    public ManeuverCandidate(
            String maneuverCandidateId,
            String encounterId,
            String targetObjectId,
            Vector3D deltaV,
            double newMinSeparationKm,
            double cost,
            int rank) {

        if (maneuverCandidateId == null || maneuverCandidateId.isBlank()) {
            throw new IllegalArgumentException("Maneuver candidate ID cannot be blank.");
        }
        if (encounterId == null || encounterId.isBlank()) {
            throw new IllegalArgumentException("Encounter ID cannot be blank.");
        }
        if (targetObjectId == null || targetObjectId.isBlank()) {
            throw new IllegalArgumentException("Target object ID cannot be blank.");
        }
        this.deltaV = Objects.requireNonNull(deltaV, "Delta-v cannot be null.");
        validateCoordinate(deltaV.getX(), "deltaV.x");
        validateCoordinate(deltaV.getY(), "deltaV.y");
        validateCoordinate(deltaV.getZ(), "deltaV.z");

        if (!Double.isFinite(newMinSeparationKm) || newMinSeparationKm < 0.0) {
            throw new IllegalArgumentException(
                    "New minimum separation must be finite and non-negative, got: " + newMinSeparationKm);
        }
        if (!Double.isFinite(cost) || cost < 0.0) {
            throw new IllegalArgumentException(
                    "Cost must be finite and non-negative, got: " + cost);
        }
        if (rank < 1) {
            throw new IllegalArgumentException("Rank must be at least 1, got: " + rank);
        }

        this.maneuverCandidateId = maneuverCandidateId;
        this.encounterId = encounterId;
        this.targetObjectId = targetObjectId;
        this.newMinSeparationKm = newMinSeparationKm;
        this.cost = cost;
        this.rank = rank;
    }

    private static void validateCoordinate(double coord, String name) {
        if (!Double.isFinite(coord)) {
            throw new IllegalArgumentException(name + " must be a finite coordinate, got: " + coord);
        }
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

    public Vector3D getDeltaV() {
        return deltaV;
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

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ManeuverCandidate that)) {
            return false;
        }
        return rank == that.rank
                && Double.compare(newMinSeparationKm, that.newMinSeparationKm) == 0
                && Double.compare(cost, that.cost) == 0
                && maneuverCandidateId.equals(that.maneuverCandidateId)
                && encounterId.equals(that.encounterId)
                && targetObjectId.equals(that.targetObjectId)
                && deltaV.equals(that.deltaV);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maneuverCandidateId, encounterId, targetObjectId, deltaV, newMinSeparationKm, cost, rank);
    }

    @Override
    public String toString() {
        return "ManeuverCandidate{" +
                "id='" + maneuverCandidateId + '\'' +
                ", encounterId='" + encounterId + '\'' +
                ", targetObjectId='" + targetObjectId + '\'' +
                ", deltaV=" + deltaV +
                ", newMinSeparationKm=" + newMinSeparationKm +
                ", cost=" + cost +
                ", rank=" + rank +
                '}';
    }
}

