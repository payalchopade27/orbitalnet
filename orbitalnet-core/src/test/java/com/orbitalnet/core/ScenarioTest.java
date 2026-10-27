
package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioTest {

    private ScenarioGenerationConfig createConfig() {
        return new ScenarioGenerationConfig(
                1, 1,
                -100, 100,
                -1, 1,
                0.1, 1.0
        );
    }

    private InitialObjectState createSatelliteState() {
        return new InitialObjectState(
                "SAT-1",
                SpaceObject.ObjectType.SATELLITE,
                new Vector3D(10, 20, 30),
                new Vector3D(1, 2, 3),
                0.5,
                SpaceObject.Status.ACTIVE,
                "Explorer",
                null
        );
    }

    @Test
    void shouldStoreScenarioDetails() {
        Scenario scenario = new Scenario(
                "SC-1",
                createConfig(),
                12345L,
                List.of(createSatelliteState()),
                Scenario.Status.READY
        );

        assertEquals("SC-1", scenario.getScenarioId());
        assertEquals(12345L, scenario.getSeed());
        assertEquals(Scenario.Status.READY, scenario.getStatus());
        assertEquals(1, scenario.getInitialObjectStates().size());
    }

    @Test
    void shouldProtectInitialStatesFromExternalListChanges() {
        List<InitialObjectState> states = new ArrayList<>();
        states.add(createSatelliteState());

        Scenario scenario = new Scenario(
                "SC-1",
                createConfig(),
                12345L,
                states,
                Scenario.Status.READY
        );

        states.clear();

        assertEquals(1, scenario.getInitialObjectStates().size());
    }

    @Test
    void shouldNotAllowModificationOfInitialStates() {
        Scenario scenario = new Scenario(
                "SC-1",
                createConfig(),
                12345L,
                List.of(createSatelliteState()),
                Scenario.Status.READY
        );

        assertThrows(UnsupportedOperationException.class, () ->
                scenario.getInitialObjectStates().clear());
    }

    @Test
    void shouldRejectBlankScenarioId() {
        assertThrows(IllegalArgumentException.class, () ->
                new Scenario(
                        " ",
                        createConfig(),
                        12345L,
                        List.of(),
                        Scenario.Status.READY
                ));
    }

    @Test
    void shouldSupportFailedStatus() {
        Scenario scenario = new Scenario(
                "SC-FAILED",
                createConfig(),
                12345L,
                List.of(),
                Scenario.Status.FAILED
        );

        assertEquals(Scenario.Status.FAILED, scenario.getStatus());
        assertTrue(scenario.getInitialObjectStates().isEmpty());
    }
}
