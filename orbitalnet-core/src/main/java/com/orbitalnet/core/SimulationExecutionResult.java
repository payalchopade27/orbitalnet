package com.orbitalnet.core;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable result model capturing the complete outcome of a baseline simulation run.
 *
 * <p>Exposes the simulation run state, tracked encounters, ranked threats, and
 * encounter relationship graph without framework dependencies.
 */
public final class SimulationExecutionResult {

    private final String runId;
    private final String scenarioId;
    private final SimulationRun simulationRun;
    private final List<Encounter> encounters;
    private final List<Encounter> rankedThreats;
    private final OrbitalGraph encounterGraph;

    public SimulationExecutionResult(
            String runId,
            String scenarioId,
            SimulationRun simulationRun,
            List<Encounter> encounters,
            List<Encounter> rankedThreats,
            OrbitalGraph encounterGraph) {

        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("Run ID cannot be blank.");
        }
        if (scenarioId == null || scenarioId.isBlank()) {
            throw new IllegalArgumentException("Scenario ID cannot be blank.");
        }

        this.runId = runId;
        this.scenarioId = scenarioId;
        this.simulationRun = Objects.requireNonNull(simulationRun, "Simulation run cannot be null.");
        this.encounters = encounters != null
                ? Collections.unmodifiableList(encounters)
                : Collections.emptyList();
        this.rankedThreats = rankedThreats != null
                ? Collections.unmodifiableList(rankedThreats)
                : Collections.emptyList();
        this.encounterGraph = encounterGraph;
    }

    public String getRunId() {
        return runId;
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public SimulationRun getSimulationRun() {
        return simulationRun;
    }

    public List<Encounter> getEncounters() {
        return encounters;
    }

    public List<Encounter> getRankedThreats() {
        return rankedThreats;
    }

    public OrbitalGraph getEncounterGraph() {
        return encounterGraph;
    }
}

