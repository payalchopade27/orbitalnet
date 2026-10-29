
package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KnownThreatScenarioTest {

    @Test
    void shouldGeneratePredictableCloseApproachScenario() {
        ScenarioGenerator generator = new ScenarioGenerator();

        Scenario scenario =
                generator.generateKnownThreatScenario(
                        "KNOWN-THREAT-1", 42L);

        assertEquals(Scenario.Status.READY, scenario.getStatus());
        assertEquals(1, scenario.getGenerationConfig().getSatelliteCount());
        assertEquals(1, scenario.getGenerationConfig().getDebrisCount());
        assertEquals(2, scenario.getInitialObjectStates().size());

        InitialObjectState satellite =
                scenario.getInitialObjectStates().get(0);
        InitialObjectState debris =
                scenario.getInitialObjectStates().get(1);

        assertEquals("SAT-1", satellite.getId());
        assertEquals("DEB-1", debris.getId());

        assertEquals(
                new Vector3D(-100, 0, 0),
                satellite.getPosition());
        assertEquals(
                new Vector3D(100, 0, 0),
                debris.getPosition());

        assertEquals(
                new Vector3D(1, 0, 0),
                satellite.getVelocity());
        assertEquals(
                new Vector3D(-1, 0, 0),
                debris.getVelocity());
    }


    @Test
    void shouldGenerateSameInitialStatesForSameSeed() {
        ScenarioGenerator generator = new ScenarioGenerator();

        Scenario first = generator.generateKnownThreatScenario(
                "KNOWN-THREAT-1", 42L);

        Scenario second = generator.generateKnownThreatScenario(
                "KNOWN-THREAT-1", 42L);

        assertEquals(
                first.getInitialObjectStates(),
                second.getInitialObjectStates());
    }

}
