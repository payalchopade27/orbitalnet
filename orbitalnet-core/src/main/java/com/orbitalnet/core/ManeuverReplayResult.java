package com.orbitalnet.core;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result model capturing the outcome of replaying a baseline simulation and applying
 * an avoidance maneuver candidate as a separate simulation run.
 *
 * <p>Exposes {@code runId}, {@code baselineRunId}, and {@code maneuverCandidateId}
 * without circular object relationships or mutated baseline encounter entities.
 */
public final class ManeuverReplayResult {

    private final String runId;
    private final String baselineRunId;
    private final String maneuverCandidateId;
    private final SimulationRun maneuverRun;
    private final List<Encounter> recalculatedEncounters;
    private final int appliedAtTick;
    private final Vector3D appliedDeltaV;
    private final String targetObjectId;

    public ManeuverReplayResult(
            String runId,
            String baselineRunId,
            String maneuverCandidateId,
            SimulationRun maneuverRun,
            List<Encounter> recalculatedEncounters,
            int appliedAtTick,
            Vector3D appliedDeltaV,
            String targetObjectId) {

        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("Run ID cannot be blank.");
        }
        if (baselineRunId == null || baselineRunId.isBlank()) {
            throw new IllegalArgumentException("Baseline run ID cannot be blank.");
        }
        if (maneuverCandidateId == null || maneuverCandidateId.isBlank()) {
            throw new IllegalArgumentException("Maneuver candidate ID cannot be blank.");
        }
        if (appliedAtTick < 0) {
            throw new IllegalArgumentException("Applied-at tick cannot be negative.");
        }

        this.runId = runId;
        this.baselineRunId = baselineRunId;
        this.maneuverCandidateId = maneuverCandidateId;
        this.maneuverRun = Objects.requireNonNull(maneuverRun, "Maneuver run cannot be null.");
        this.recalculatedEncounters = recalculatedEncounters != null
                ? Collections.unmodifiableList(recalculatedEncounters)
                : Collections.emptyList();
        this.appliedAtTick = appliedAtTick;
        this.appliedDeltaV = Objects.requireNonNull(appliedDeltaV, "Applied delta-v cannot be null.");
        this.targetObjectId = Objects.requireNonNull(targetObjectId, "Target object ID cannot be null.");
    }

    public String getRunId() {
        return runId;
    }

    public String getBaselineRunId() {
        return baselineRunId;
    }

    public String getManeuverCandidateId() {
        return maneuverCandidateId;
    }

    public SimulationRun getManeuverRun() {
        return maneuverRun;
    }

    public List<Encounter> getRecalculatedEncounters() {
        return recalculatedEncounters;
    }

    public int getAppliedAtTick() {
        return appliedAtTick;
    }

    public Vector3D getAppliedDeltaV() {
        return appliedDeltaV;
    }

    public String getTargetObjectId() {
        return targetObjectId;
    }
}
