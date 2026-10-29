
package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioGeneratorTest {

    @Test
    void sameSeedAndConfigShouldProduceSameInitialStates() {
        ScenarioGenerationConfig config = new ScenarioGenerationConfig(
                3, 4, -1000, 1000, -8, 8, 0.1, 1.0
        );

        ScenarioGenerator generator = new ScenarioGenerator();

        Scenario first = generator.generate("SC-1", config, 12345L);
        Scenario second = generator.generate("SC-2", config, 12345L);

        assertEquals(
                first.getInitialObjectStates(),
                second.getInitialObjectStates()
        );
    }

    @Test
    void shouldGenerateConfiguredCountsAndStableIds() {
        ScenarioGenerationConfig config = new ScenarioGenerationConfig(
                2, 3, -100, 100, -2, 2, 0.1, 1.0
        );

        Scenario scenario = new ScenarioGenerator().generate(
                "SC-1", config, 42L
        );

        List<InitialObjectState> states = scenario.getInitialObjectStates();

        assertEquals(5, states.size());
        assertEquals("SAT-1", states.get(0).getId());
        assertEquals("SAT-2", states.get(1).getId());
        assertEquals("DEB-1", states.get(2).getId());
        assertEquals("DEB-2", states.get(3).getId());
        assertEquals("DEB-3", states.get(4).getId());
    }

    @Test
    void shouldKeepGeneratedValuesWithinConfiguredBounds() {
        ScenarioGenerationConfig config = new ScenarioGenerationConfig(
                5, 5, -100, 100, -2, 2, 0.1, 1.0
        );

        Scenario scenario = new ScenarioGenerator().generate(
                "SC-1", config, 42L
        );

        for (InitialObjectState state : scenario.getInitialObjectStates()) {
            assertWithin(state.getPosition().getX(), -100, 100);
            assertWithin(state.getPosition().getY(), -100, 100);
            assertWithin(state.getPosition().getZ(), -100, 100);

            assertWithin(state.getVelocity().getX(), -2, 2);
            assertWithin(state.getVelocity().getY(), -2, 2);
            assertWithin(state.getVelocity().getZ(), -2, 2);

            assertWithin(state.getSize(), 0.1, 1.0);
        }
    }

    @Test
    void shouldReconstructFreshObjectsWithoutChangingScenario() {
        Scenario scenario = new ScenarioGenerator().generate(
                "SC-1",
                new ScenarioGenerationConfig(
                        1, 1, -100, 100, -2, 2, 0.1, 1.0
                ),
                42L
        );

        InitialObjectState original = scenario.getInitialObjectStates().get(0);
        SpaceObject first = original.toSpaceObject();
        SpaceObject second = original.toSpaceObject();

        assertNotEquals(first, second);
        assertEquals(original.getPosition(), first.getPosition());
        assertEquals(original.getPosition(), second.getPosition());

        first.setPosition(new Vector3D(999, 999, 999));

        assertEquals(original.getPosition(), second.getPosition());
        assertEquals(
                new Vector3D(999, 999, 999),
                first.getPosition()
        );
    }

    @Test
    void shouldMarkGeneratedScenarioReady() {
        Scenario scenario = new ScenarioGenerator().generate(
                "SC-1",
                new ScenarioGenerationConfig(
                        1, 1, -100, 100, -2, 2, 0.1, 1.0
                ),
                42L
        );

        assertEquals(Scenario.Status.READY, scenario.getStatus());
        assertEquals(42L, scenario.getSeed());
    }

    @Test
    void shouldSupportAnEmptyScenario() {
        Scenario scenario = new ScenarioGenerator().generate(
                "SC-EMPTY",
                new ScenarioGenerationConfig(
                        0, 0, -100, 100, -2, 2, 0.1, 1.0
                ),
                42L
        );

        assertTrue(scenario.getInitialObjectStates().isEmpty());
    }

    private void assertWithin(double value, double minimum, double maximum) {
        assertTrue(value >= minimum && value <= maximum);
    }
}
