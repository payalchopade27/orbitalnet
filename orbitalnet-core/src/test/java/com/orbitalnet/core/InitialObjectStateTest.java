
package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InitialObjectStateTest {

    @Test
    void shouldStoreSatelliteStateAndOrbit() {
        Vector3D position = new Vector3D(100, 200, 300);
        Vector3D velocity = new Vector3D(1, 2, 3);
        Orbit orbit = new Orbit(500, 45, 6000);

        InitialObjectState state = new InitialObjectState(
                "SAT-1",
                SpaceObject.ObjectType.SATELLITE,
                position,
                velocity,
                0.5,
                SpaceObject.Status.ACTIVE,
                "Explorer",
                orbit
        );

        assertEquals("SAT-1", state.getId());
        assertEquals(SpaceObject.ObjectType.SATELLITE, state.getType());
        assertEquals(position, state.getPosition());
        assertEquals(velocity, state.getVelocity());
        assertEquals(0.5, state.getSize());
        assertEquals(SpaceObject.Status.ACTIVE, state.getStatus());
        assertEquals("Explorer", state.getName());
        assertEquals(orbit, state.getOrbit());
    }

    @Test
    void shouldStoreDebrisWithoutSatelliteMetadata() {
        InitialObjectState state = new InitialObjectState(
                "DEB-1",
                SpaceObject.ObjectType.DEBRIS,
                new Vector3D(0, 0, 0),
                new Vector3D(0, 0, 0),
                0.1,
                SpaceObject.Status.ACTIVE,
                null,
                null
        );

        assertNull(state.getName());
        assertNull(state.getOrbit());
    }

    @Test
    void shouldRejectBlankId() {
        assertThrows(IllegalArgumentException.class, () ->
                new InitialObjectState(
                        " ",
                        SpaceObject.ObjectType.DEBRIS,
                        new Vector3D(0, 0, 0),
                        new Vector3D(0, 0, 0),
                        0.1,
                        SpaceObject.Status.ACTIVE,
                        null,
                        null
                ));
    }

    @Test
    void shouldRejectInvalidSize() {
        assertThrows(IllegalArgumentException.class, () ->
                new InitialObjectState(
                        "DEB-1",
                        SpaceObject.ObjectType.DEBRIS,
                        new Vector3D(0, 0, 0),
                        new Vector3D(0, 0, 0),
                        0,
                        SpaceObject.Status.ACTIVE,
                        null,
                        null
                ));
    }

    @Test
    void shouldRejectNullPosition() {
        assertThrows(NullPointerException.class, () ->
                new InitialObjectState(
                        "SAT-1",
                        SpaceObject.ObjectType.SATELLITE,
                        null,
                        new Vector3D(0, 0, 0),
                        0.5,
                        SpaceObject.Status.ACTIVE,
                        "Explorer",
                        null
                ));
    }

    @Test
    void shouldRejectSatelliteWithoutName() {
        assertThrows(IllegalArgumentException.class, () ->
                new InitialObjectState(
                        "SAT-1",
                        SpaceObject.ObjectType.SATELLITE,
                        new Vector3D(0, 0, 0),
                        new Vector3D(0, 0, 0),
                        0.5,
                        SpaceObject.Status.ACTIVE,
                        null,
                        null
                ));
    }

    @Test
    void shouldRejectSatelliteMetadataForDebris() {
        assertThrows(IllegalArgumentException.class, () ->
                new InitialObjectState(
                        "DEB-1",
                        SpaceObject.ObjectType.DEBRIS,
                        new Vector3D(0, 0, 0),
                        new Vector3D(0, 0, 0),
                        0.1,
                        SpaceObject.Status.ACTIVE,
                        "Not allowed",
                        null
                ));
    }


    @Test
    void shouldReconstructSatellite() {
        Orbit orbit = new Orbit(500, 45, 6000);

        InitialObjectState state = new InitialObjectState(
                "SAT-1",
                SpaceObject.ObjectType.SATELLITE,
                new Vector3D(10, 20, 30),
                new Vector3D(1, 2, 3),
                0.5,
                SpaceObject.Status.ACTIVE,
                "Explorer",
                orbit
        );

        SpaceObject reconstructed = state.toSpaceObject();

        assertTrue(reconstructed instanceof Satellite);

        Satellite satellite = (Satellite) reconstructed;
        assertEquals("SAT-1", satellite.getId());
        assertEquals("Explorer", satellite.getName());
        assertEquals(orbit, satellite.getOrbit());
        assertEquals(new Vector3D(10, 20, 30), satellite.getPosition());
    }

    @Test
    void shouldReconstructDebris() {
        InitialObjectState state = new InitialObjectState(
                "DEB-1",
                SpaceObject.ObjectType.DEBRIS,
                new Vector3D(10, 20, 30),
                new Vector3D(1, 2, 3),
                0.1,
                SpaceObject.Status.ACTIVE,
                null,
                null
        );

        SpaceObject reconstructed = state.toSpaceObject();

        assertTrue(reconstructed instanceof Debris);
        assertEquals("DEB-1", reconstructed.getId());
        assertEquals(new Vector3D(10, 20, 30), reconstructed.getPosition());
    }

}
