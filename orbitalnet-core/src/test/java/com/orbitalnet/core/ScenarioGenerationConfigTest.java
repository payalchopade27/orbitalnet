
package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScenarioGenerationConfigTest {

    @Test
    void shouldStoreGenerationSettings() {
        ScenarioGenerationConfig config = new ScenarioGenerationConfig(
                10, 20,
                -1000, 1000,
                -8, 8,
                0.1, 2.0
        );

        assertEquals(10, config.getSatelliteCount());
        assertEquals(20, config.getDebrisCount());
        assertEquals(-1000, config.getMinPositionKm());
        assertEquals(1000, config.getMaxPositionKm());
        assertEquals(-8, config.getMinVelocityKmPerSec());
        assertEquals(8, config.getMaxVelocityKmPerSec());
        assertEquals(0.1, config.getMinSizeKm());
        assertEquals(2.0, config.getMaxSizeKm());
    }

    @Test
    void shouldAllowZeroObjectCounts() {
        ScenarioGenerationConfig config = new ScenarioGenerationConfig(
                0, 0, -100, 100, -1, 1, 0.1, 1.0
        );

        assertEquals(0, config.getSatelliteCount());
        assertEquals(0, config.getDebrisCount());
    }

    @Test
    void shouldRejectNegativeObjectCounts() {
        assertThrows(IllegalArgumentException.class, () ->
                new ScenarioGenerationConfig(
                        -1, 2, -100, 100, -1, 1, 0.1, 1.0
                ));
    }

    @Test
    void shouldRejectInvalidRanges() {
        assertThrows(IllegalArgumentException.class, () ->
                new ScenarioGenerationConfig(
                        1, 1, 100, -100, -1, 1, 0.1, 1.0
                ));
    }

    @Test
    void shouldRejectNonFiniteBounds() {
        assertThrows(IllegalArgumentException.class, () ->
                new ScenarioGenerationConfig(
                        1, 1, -100, 100,
                        Double.NaN, 1, 0.1, 1.0
                ));
    }

    @Test
    void shouldRejectNonPositiveObjectSize() {
        assertThrows(IllegalArgumentException.class, () ->
                new ScenarioGenerationConfig(
                        1, 1, -100, 100, -1, 1, 0, 1.0
                ));
    }
}
