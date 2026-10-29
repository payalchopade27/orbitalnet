
package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebrisTest {

    @Test
    void shouldStoreDebrisProperties() {
        Vector3D position = new Vector3D(100, 200, 300);
        Vector3D velocity = new Vector3D(2, 1, 0);

        Debris debris = new Debris(
                "DEB-1",
                position,
                velocity,
                0.5,
                SpaceObject.Status.ACTIVE
        );

        assertEquals("DEB-1", debris.getId());
        assertEquals(SpaceObject.ObjectType.DEBRIS, debris.getType());
        assertEquals(position, debris.getPosition());
        assertEquals(velocity, debris.getVelocity());
        assertEquals(0.5, debris.getSize());
        assertTrue(debris.isActive());
    }
}
