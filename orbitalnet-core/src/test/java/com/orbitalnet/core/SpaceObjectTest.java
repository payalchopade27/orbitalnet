
package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpaceObjectTest {

    private static class TestSpaceObject extends SpaceObject {

        TestSpaceObject(
                String id,
                Vector3D position,
                Vector3D velocity,
                double size,
                Status status) {

            super(id, ObjectType.SATELLITE, position, velocity, size, status);
        }
    }

    @Test
    void shouldStoreObjectProperties() {
        Vector3D position = new Vector3D(1, 2, 3);
        Vector3D velocity = new Vector3D(4, 5, 6);

        SpaceObject object = new TestSpaceObject(
                "SAT-01", position, velocity, 2.5, SpaceObject.Status.ACTIVE);

        assertEquals("SAT-01", object.getId());
        assertEquals(SpaceObject.ObjectType.SATELLITE, object.getType());
        assertSame(position, object.getPosition());
        assertSame(velocity, object.getVelocity());
        assertEquals(2.5, object.getSize());
        assertEquals(SpaceObject.Status.ACTIVE, object.getStatus());
    }

    @Test
    void shouldReportWhetherObjectIsActive() {
        SpaceObject active = new TestSpaceObject(
                "SAT-01", new Vector3D(0, 0, 0),
                new Vector3D(1, 0, 0), 2.5, SpaceObject.Status.ACTIVE);

        SpaceObject inactive = new TestSpaceObject(
                "SAT-02", new Vector3D(0, 0, 0),
                new Vector3D(1, 0, 0), 2.5, SpaceObject.Status.INACTIVE);

        assertTrue(active.isActive());
        assertFalse(inactive.isActive());
    }

    @Test
    void shouldAllowPositionVelocityAndStatusUpdates() {
        SpaceObject object = new TestSpaceObject(
                "SAT-01", new Vector3D(0, 0, 0),
                new Vector3D(1, 0, 0), 2.5, SpaceObject.Status.ACTIVE);

        Vector3D newPosition = new Vector3D(10, 20, 30);
        Vector3D newVelocity = new Vector3D(2, 3, 4);

        object.setPosition(newPosition);
        object.setVelocity(newVelocity);
        object.setStatus(SpaceObject.Status.DESTROYED);

        assertSame(newPosition, object.getPosition());
        assertSame(newVelocity, object.getVelocity());
        assertEquals(SpaceObject.Status.DESTROYED, object.getStatus());
        assertFalse(object.isActive());
    }


    @Test
    void shouldMoveActiveObjectUsingVelocityAndTime() {
        SpaceObject object = new TestSpaceObject(
                "SAT-01",
                new Vector3D(10, 20, 30),
                new Vector3D(1, 0, -2),
                2.5,
                SpaceObject.Status.ACTIVE);

        object.advancePosition(5.0);

        assertEquals(new Vector3D(15, 20, 20), object.getPosition());
        assertEquals(new Vector3D(1, 0, -2), object.getVelocity());
    }

    @Test
    void shouldNotMoveInactiveObject() {
        Vector3D initialPosition = new Vector3D(10, 20, 30);

        SpaceObject object = new TestSpaceObject(
                "SAT-02",
                initialPosition,
                new Vector3D(1, 0, -2),
                2.5,
                SpaceObject.Status.INACTIVE);

        object.advancePosition(5.0);

        assertEquals(initialPosition, object.getPosition());
    }

    @Test
    void shouldNotMoveDestroyedObject() {
        Vector3D initialPosition = new Vector3D(10, 20, 30);

        SpaceObject object = new TestSpaceObject(
                "SAT-03",
                initialPosition,
                new Vector3D(1, 0, -2),
                2.5,
                SpaceObject.Status.DESTROYED);

        object.advancePosition(5.0);

        assertEquals(initialPosition, object.getPosition());
    }

}
