
package com.orbitalnet.core;

import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SimulationRun {

    public enum Status {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED
    }

    private final String runId;
    private final String scenarioId;
    private final Scenario scenario;
    private final String baselineRunId;
    private final String maneuverCandidateId;
    private final List<SpaceObject> objects;

    private Status status;
    private int currentTick;
    private double elapsedTimeSeconds;

    public SimulationRun(
            String runId,
            Scenario scenario,
            String baselineRunId,
            String maneuverCandidateId) {

        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException(
                    "Run ID cannot be blank.");
        }

        this.scenario = Objects.requireNonNull(
                scenario, "Scenario cannot be null.");

        if (scenario.getStatus() != Scenario.Status.READY) {
            throw new IllegalArgumentException(
                    "Cannot create a run from a scenario that is not READY.");
        }

        this.runId = runId;
        this.scenarioId = scenario.getScenarioId();
        this.baselineRunId = baselineRunId;
        this.maneuverCandidateId = maneuverCandidateId;

        List<SpaceObject> workingObjects = new ArrayList<>();

        for (InitialObjectState initialState : scenario.getInitialObjectStates()) {
            workingObjects.add(initialState.toSpaceObject());
        }

        this.objects = workingObjects;

        this.status = Status.PENDING;
        this.currentTick = 0;
        this.elapsedTimeSeconds = 0.0;
    }

    public String getRunId() {
        return runId;
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public List<SpaceObject> getObjects() {
        return Collections.unmodifiableList(objects);
    }

    public Status getStatus() {
        return status;
    }

    public int getCurrentTick() {
        return currentTick;
    }

    public double getElapsedTimeSeconds() {
        return elapsedTimeSeconds;
    }

    public String getBaselineRunId() {
        return baselineRunId;
    }

    public String getManeuverCandidateId() {
        return maneuverCandidateId;
    }


    public void advanceTick() {
        if (status != Status.RUNNING) {
            throw new IllegalStateException(
                    "Only a RUNNING run can advance.");
        }

        for (SpaceObject object : objects) {
            object.advancePosition(
                    SimulationConstants.DELTA_TIME_SECONDS);
        }

        currentTick++;
        elapsedTimeSeconds =
                currentTick * SimulationConstants.DELTA_TIME_SECONDS;

        if (elapsedTimeSeconds >=
                SimulationConstants.SIMULATION_HORIZON_SECONDS) {
            complete();
        }
    }

    public void start() {
        if (status != Status.PENDING) {
            throw new IllegalStateException(
                    "Only a PENDING run can be started.");
        }

        status = Status.RUNNING;
    }

    public void complete() {
        if (status != Status.RUNNING) {
            throw new IllegalStateException(
                    "Only a RUNNING run can be completed.");
        }

        status = Status.COMPLETED;
    }


    public void fail() {
        if (status != Status.PENDING && status != Status.RUNNING) {
            throw new IllegalStateException(
                    "Only a PENDING or RUNNING run can be failed.");
        }

        status = Status.FAILED;
    }

}
