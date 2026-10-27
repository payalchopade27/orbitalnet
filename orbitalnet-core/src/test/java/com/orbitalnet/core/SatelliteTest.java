
package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SatelliteTest {

    @Test
    void shouldStoreSatelliteProperties() {
        Vector3D position = new Vector3D(10, 20, 30);
        Vector3D velocity = new Vector3D(1, 2, 3);
        Orbit orbit = new Orbit(550.0, 53.0, 5700.0);

        Satellite satellite = new Satellite(
                "SAT-1",
                "Test Satellite",
                position,
                velocity,
                2.0,
                SpaceObject.Status.ACTIVE,
                orbit
        );

        assertEquals("SAT-1", satellite.getId());
        assertEquals("Test Satellite", satellite.getName());
        assertEquals(SpaceObject.ObjectType.SATELLITE, satellite.getType());
        assertSame(position, satellite.getPosition());
        assertSame(velocity, satellite.getVelocity());
        assertEquals(2.0, satellite.getSize());
        assertTrue(satellite.isActive());
        assertSame(orbit, satellite.getOrbit());
    }

    @Test
    void shouldAllowMissingOrbitMetadata() {
        Satellite satellite = new Satellite(
                "SAT-2",
                "Satellite Without Orbit",
                new Vector3D(0, 0, 0),
                new Vector3D(1, 0, 0),
                1.0,
                SpaceObject.Status.ACTIVE,
                null
        );

        assertEquals(null, satellite.getOrbit());
    }
}
