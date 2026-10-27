
package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimulationRunTest {

    private Scenario createScenario() {
        ScenarioGenerationConfig config =
                new ScenarioGenerationConfig(
                        0, 0, -100, 100, -1, 1, 1.0, 1.0);

        return new Scenario(
                "SCENARIO-1",
                config,
                42L,
                java.util.Collections.emptyList(),
                Scenario.Status.READY);
    }

    private Scenario createScenarioWithObject() {
    ScenarioGenerationConfig config =
            new ScenarioGenerationConfig(
                    1, 0, -100, 100, -1, 1, 1.0, 1.0);

    InitialObjectState initialState = new InitialObjectState(
            "SAT-01",
            SpaceObject.ObjectType.SATELLITE,
            new Vector3D(10, 20, 30),
            new Vector3D(1, 0, 0),
            1.0,
            SpaceObject.Status.ACTIVE,
            "Test Satellite",
            null);

    return new Scenario(
            "SCENARIO-2",
            config,
            42L,
            java.util.Collections.singletonList(initialState),
            Scenario.Status.READY);
    }

    @Test
    void shouldCompleteRunThroughValidTransitions() {
        SimulationRun run = new SimulationRun(
                "RUN-1", createScenario(), null, null);

        assertEquals(SimulationRun.Status.PENDING, run.getStatus());

        run.start();
        assertEquals(SimulationRun.Status.RUNNING, run.getStatus());

        run.complete();
        assertEquals(SimulationRun.Status.COMPLETED, run.getStatus());
    }

    @Test
    void shouldAllowFailingPendingRun() {
        SimulationRun run = new SimulationRun(
                "RUN-2", createScenario(), null, null);

        run.fail();

        assertEquals(SimulationRun.Status.FAILED, run.getStatus());
    }

    @Test
    void shouldAllowFailingRunningRun() {
        SimulationRun run = new SimulationRun(
                "RUN-3", createScenario(), null, null);

        run.start();
        run.fail();

        assertEquals(SimulationRun.Status.FAILED, run.getStatus());
    }

    @Test
    void shouldRejectStartingRunMoreThanOnce() {
        SimulationRun run = new SimulationRun(
                "RUN-4", createScenario(), null, null);

        run.start();

        assertThrows(IllegalStateException.class, run::start);
    }

    @Test
    void shouldRejectCompletingPendingRun() {
        SimulationRun run = new SimulationRun(
                "RUN-5", createScenario(), null, null);

        assertThrows(IllegalStateException.class, run::complete);
    }

    @Test
    void shouldRejectFailingCompletedRun() {
        SimulationRun run = new SimulationRun(
                "RUN-6", createScenario(), null, null);

        run.start();
        run.complete();

        assertThrows(IllegalStateException.class, run::fail);
    }

    @Test
    void shouldAdvanceTickAndElapsedTime() {
        SimulationRun run = new SimulationRun(
                "run-1", createScenario(), null, null);

        run.start();
        run.advanceTick();

        assertEquals(1, run.getCurrentTick());
        assertEquals(5.0, run.getElapsedTimeSeconds());

        run.advanceTick();
        run.advanceTick();

        assertEquals(3, run.getCurrentTick());
        assertEquals(15.0, run.getElapsedTimeSeconds());
    }

    @Test
    void shouldStartAtTickZero() {
        SimulationRun run = new SimulationRun(
                "run-1", createScenario(), null, null);

        assertEquals(0, run.getCurrentTick());
        assertEquals(0.0, run.getElapsedTimeSeconds());
    }

    @Test
    void shouldRejectTickAdvancementWhenNotRunning() {
        SimulationRun run = new SimulationRun(
                "run-1", createScenario(), null, null);

        assertThrows(
                IllegalStateException.class,
                run::advanceTick);
    }

    @Test
    void shouldCreateWorkingObjectsFromScenario() {
        Scenario scenario = createScenarioWithObject();
        SimulationRun run = new SimulationRun(
                "RUN-10", scenario, null, null);

        assertEquals(1, run.getObjects().size());
        assertEquals("SAT-01", run.getObjects().get(0).getId());
        assertEquals(
                new Vector3D(10, 20, 30),
                run.getObjects().get(0).getPosition());

        assertEquals(
                new Vector3D(10, 20, 30),
                scenario.getInitialObjectStates().get(0)
                        .getPosition());
    }

    @Test
    void shouldKeepWorkingObjectsIndependentBetweenRuns() {
        Scenario scenario = createScenarioWithObject();

        SimulationRun firstRun = new SimulationRun(
                "RUN-11", scenario, null, null);
        SimulationRun secondRun = new SimulationRun(
                "RUN-12", scenario, null, null);

        assertNotSame(
            firstRun.getObjects().get(0),
            secondRun.getObjects().get(0));

        firstRun.getObjects().get(0).setPosition(
                new Vector3D(99, 99, 99));

        assertEquals(
                new Vector3D(99, 99, 99),
                firstRun.getObjects().get(0).getPosition());

        assertEquals(
                new Vector3D(10, 20, 30),
                secondRun.getObjects().get(0).getPosition());

        assertEquals(
                new Vector3D(10, 20, 30),
                scenario.getInitialObjectStates().get(0)
                        .getPosition());
    }


    @Test
    void shouldMoveObjectsWhenAdvancingTick() {
        SimulationRun run = new SimulationRun(
                "RUN-13", createScenarioWithObject(), null, null);

        run.start();
        run.advanceTick();

        assertEquals(
                new Vector3D(15, 20, 30),
                run.getObjects().get(0).getPosition());

        assertEquals(1, run.getCurrentTick());
        assertEquals(5.0, run.getElapsedTimeSeconds());
    }


    @Test
    void shouldCompleteRunAtSimulationHorizon() {
        SimulationRun run = new SimulationRun(
                "RUN-14", createScenario(), null, null);

        run.start();

        for (int i = 0; i < 720; i++) {
            run.advanceTick();
        }

        assertEquals(SimulationRun.Status.COMPLETED, run.getStatus());
        assertEquals(720, run.getCurrentTick());
        assertEquals(3600.0, run.getElapsedTimeSeconds());
    }

    @Test
    void shouldNotAdvanceBeyondSimulationHorizon() {
        SimulationRun run = new SimulationRun(
                "RUN-15", createScenario(), null, null);

        run.start();

        for (int i = 0; i < 720; i++) {
            run.advanceTick();
        }

        assertThrows(IllegalStateException.class, run::advanceTick);

        assertEquals(720, run.getCurrentTick());
        assertEquals(3600.0, run.getElapsedTimeSeconds());
    }

    @Test
    void shouldNotMoveInactiveObjectDuringTick() {
        SimulationRun run = new SimulationRun(
                "RUN-16", createScenarioWithObject(), null, null);

        run.start();

        SpaceObject object = run.getObjects().get(0);
        object.setStatus(SpaceObject.Status.INACTIVE);

        Vector3D initialPosition = object.getPosition();

        run.advanceTick();

        assertEquals(initialPosition, object.getPosition());
        assertEquals(1, run.getCurrentTick());
        assertEquals(5.0, run.getElapsedTimeSeconds());
    }

    @Test
    void shouldNotMoveDestroyedObjectDuringTick() {
        SimulationRun run = new SimulationRun(
                "RUN-17", createScenarioWithObject(), null, null);

        run.start();

        SpaceObject object = run.getObjects().get(0);
        object.setStatus(SpaceObject.Status.DESTROYED);

        Vector3D initialPosition = object.getPosition();

        run.advanceTick();

        assertEquals(initialPosition, object.getPosition());
        assertEquals(1, run.getCurrentTick());
        assertEquals(5.0, run.getElapsedTimeSeconds());
    }
}
