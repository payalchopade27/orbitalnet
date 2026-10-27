package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrbitTest {

    @Test
    void shouldStoreOrbitMetadata() {
        Orbit orbit = new Orbit(550.0, 53.0, 5700.0);

        assertEquals(550.0, orbit.getAltitudeKm());
        assertEquals(53.0, orbit.getInclinationDeg());
        assertEquals(5700.0, orbit.getPeriodSeconds());
    }
}
