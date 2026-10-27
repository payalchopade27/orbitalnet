package com.orbitalnet.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Service engine for generating, evaluating, and ranking candidate collision avoidance maneuvers.
 *
 * <p>Evaluation uses the shared {@link ClosestApproach} trajectory prediction logic
 * without modifying the baseline objects or simulation run.
 *
 * <p>Ranking rule:
 * <ol>
 *   <li>Safety-first: {@code newMinSeparationKm} descending (largest separation distance achieved = rank 1).</li>
 *   <li>Tie-breaker: {@code cost} ascending (lowest delta-v fuel cost).</li>
 *   <li>Tie-breaker: {@code maneuverCandidateId} ascending lexicographically.</li>
 * </ol>
 */
public final class ManeuverPlanner {

    public static final double DEFAULT_DELTA_V_MAGNITUDE_KM_S = 0.010;

    public ManeuverPlanner() {
    }

    /**
     * Generates and evaluates the 3 deterministic orthogonal candidate maneuvers
     * (Normal, Radial, and Prograde) for the specified encounter and target object.
     *
     * @param encounter the close-approach encounter to mitigate
     * @param targetObject the space object executing the maneuver
     * @param otherObject the second space object involved in the encounter
     * @return ranked list of maneuver candidates, with rank 1 being the top recommendation
     */
    public List<ManeuverCandidate> planManeuvers(
            Encounter encounter,
            SpaceObject targetObject,
            SpaceObject otherObject) {

        return planManeuvers(
                encounter,
                targetObject,
                otherObject,
                DEFAULT_DELTA_V_MAGNITUDE_KM_S
        );
    }

    /**
     * Generates and evaluates the 3 deterministic candidate maneuvers with a custom impulse magnitude.
     */
    public List<ManeuverCandidate> planManeuvers(
            Encounter encounter,
            SpaceObject targetObject,
            SpaceObject otherObject,
            double deltaVMagnitudeKmS) {

        validateEncounterAndObjects(encounter, targetObject, otherObject);

        List<Vector3D> candidateDeltaVs = generateDeterministicDeltaVs(
                targetObject.getVelocity(),
                targetObject.getPosition(),
                deltaVMagnitudeKmS
        );

        return evaluateAndRankCandidates(encounter, targetObject, otherObject, candidateDeltaVs);
    }

    /**
     * Evaluates a caller-provided set of candidate delta-v vectors for an encounter.
     */
    public List<ManeuverCandidate> planManeuvers(
            Encounter encounter,
            SpaceObject targetObject,
            SpaceObject otherObject,
            List<Vector3D> candidateVectors) {

        validateEncounterAndObjects(encounter, targetObject, otherObject);
        Objects.requireNonNull(candidateVectors, "Candidate vectors list cannot be null.");

        if (candidateVectors.isEmpty()) {
            return Collections.emptyList();
        }

        return evaluateAndRankCandidates(encounter, targetObject, otherObject, candidateVectors);
    }

    /**
     * Evaluates a single maneuver candidate without modifying baseline space objects.
     */
    public ManeuverCandidate evaluateCandidate(
            String candidateId,
            String encounterId,
            SpaceObject targetObject,
            SpaceObject otherObject,
            Vector3D deltaV,
            int temporaryRank) {

        Objects.requireNonNull(targetObject, "Target object cannot be null.");
        Objects.requireNonNull(otherObject, "Other object cannot be null.");
        Objects.requireNonNull(deltaV, "Delta-v cannot be null.");

        Vector3D maneuveredVelocity = targetObject.getVelocity().add(deltaV);

        ClosestApproach closestApproach = new ClosestApproach(
                targetObject.getPosition(),
                maneuveredVelocity,
                otherObject.getPosition(),
                otherObject.getVelocity()
        );

        double newMinSeparation = closestApproach.getDistanceKm();
        double cost = deltaV.magnitude();

        return new ManeuverCandidate(
                candidateId,
                encounterId,
                targetObject.getId(),
                deltaV,
                newMinSeparation,
                cost,
                temporaryRank
        );
    }

    /**
     * Computes 3 deterministic orthogonal delta-v vectors relative to target trajectory:
     * <ol>
     *   <li>Normal / Out-of-plane vector</li>
     *   <li>Radial vector</li>
     *   <li>Prograde vector</li>
     * </ol>
     */
    public static List<Vector3D> generateDeterministicDeltaVs(
            Vector3D velocity,
            Vector3D position,
            double magnitudeKmS) {

        if (!Double.isFinite(magnitudeKmS) || magnitudeKmS <= 0.0) {
            throw new IllegalArgumentException(
                    "Delta-v magnitude must be finite and positive, got: " + magnitudeKmS);
        }

        // 1. Prograde unit vector along velocity
        Vector3D progradeUnit;
        if (velocity != null && velocity.magnitude() > 1e-9) {
            progradeUnit = velocity.scale(1.0 / velocity.magnitude());
        } else {
            progradeUnit = new Vector3D(1.0, 0.0, 0.0);
        }

        // 2. Normal unit vector perpendicular to prograde
        Vector3D refAxis = Math.abs(progradeUnit.getX()) < 0.9
                ? new Vector3D(1.0, 0.0, 0.0)
                : new Vector3D(0.0, 1.0, 0.0);

        Vector3D normalCross = cross(progradeUnit, refAxis);
        Vector3D normalUnit = normalCross.scale(1.0 / normalCross.magnitude());

        // 3. Radial unit vector perpendicular to both prograde and normal
        Vector3D radialCross = cross(normalUnit, progradeUnit);
        Vector3D radialUnit = radialCross.scale(1.0 / radialCross.magnitude());

        List<Vector3D> candidates = new ArrayList<>(3);
        candidates.add(normalUnit.scale(magnitudeKmS));
        candidates.add(radialUnit.scale(magnitudeKmS));
        candidates.add(progradeUnit.scale(magnitudeKmS));

        return candidates;
    }

    private List<ManeuverCandidate> evaluateAndRankCandidates(
            Encounter encounter,
            SpaceObject targetObject,
            SpaceObject otherObject,
            List<Vector3D> candidateVectors) {

        List<ManeuverCandidate> unranked = new ArrayList<>(candidateVectors.size());

        for (int i = 0; i < candidateVectors.size(); i++) {
            Vector3D dv = candidateVectors.get(i);
            String candidateId = String.format("%s-MAN-%02d", encounter.getEncounterId(), i + 1);

            ManeuverCandidate candidate = evaluateCandidate(
                    candidateId,
                    encounter.getEncounterId(),
                    targetObject,
                    otherObject,
                    dv,
                    1 // Temporary rank
            );
            unranked.add(candidate);
        }

        // Sort candidates:
        // 1. newMinSeparationKm DESC (safety-first)
        // 2. cost ASC (lowest delta-v fuel cost)
        // 3. candidateId ASC (deterministic tie-breaking)
        CustomSorter.mergeSort(unranked, (a, b) -> {
            int sepComp = Double.compare(b.getNewMinSeparationKm(), a.getNewMinSeparationKm());
            if (sepComp != 0) {
                return sepComp;
            }
            int costComp = Double.compare(a.getCost(), b.getCost());
            if (costComp != 0) {
                return costComp;
            }
            return a.getManeuverCandidateId().compareTo(b.getManeuverCandidateId());
        });

        // Assign unique 1-based ranks
        List<ManeuverCandidate> ranked = new ArrayList<>(unranked.size());
        for (int i = 0; i < unranked.size(); i++) {
            ManeuverCandidate c = unranked.get(i);
            ranked.add(new ManeuverCandidate(
                    c.getManeuverCandidateId(),
                    c.getEncounterId(),
                    c.getTargetObjectId(),
                    c.getDeltaV(),
                    c.getNewMinSeparationKm(),
                    c.getCost(),
                    i + 1
            ));
        }

        return ranked;
    }

    private static void validateEncounterAndObjects(
            Encounter encounter,
            SpaceObject targetObject,
            SpaceObject otherObject) {

        Objects.requireNonNull(encounter, "Encounter cannot be null.");
        Objects.requireNonNull(targetObject, "Target object cannot be null.");
        Objects.requireNonNull(otherObject, "Other object cannot be null.");

        String targetId = targetObject.getId();
        String otherId = otherObject.getId();

        if (targetId.equals(otherId)) {
            throw new IllegalArgumentException(
                    "Target object and other object cannot be the same object.");
        }

        boolean targetInEncounter = targetId.equals(encounter.getObjectAId())
                || targetId.equals(encounter.getObjectBId());
        boolean otherInEncounter = otherId.equals(encounter.getObjectAId())
                || otherId.equals(encounter.getObjectBId());

        if (!targetInEncounter) {
            throw new IllegalArgumentException(
                    "Target object " + targetId + " does not belong to encounter "
                            + encounter.getEncounterId());
        }

        if (!otherInEncounter) {
            throw new IllegalArgumentException(
                    "Other object " + otherId + " does not belong to encounter "
                            + encounter.getEncounterId());
        }

        if (!targetObject.isActive()) {
            throw new IllegalArgumentException(
                    "Target object " + targetId + " must be active to perform a maneuver.");
        }

        // If encounter involves an active Satellite and Debris, only the Satellite can maneuver
        if (targetObject instanceof Debris && otherObject instanceof Satellite) {
            throw new IllegalArgumentException(
                    "Debris cannot perform an active avoidance maneuver; target must be a satellite.");
        }
    }

    private static Vector3D cross(Vector3D a, Vector3D b) {
        return new Vector3D(
                a.getY() * b.getZ() - a.getZ() * b.getY(),
                a.getZ() * b.getX() - a.getX() * b.getZ(),
                a.getX() * b.getY() - a.getY() * b.getX()
        );
    }
}

