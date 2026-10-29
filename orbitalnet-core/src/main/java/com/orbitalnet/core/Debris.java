
package com.orbitalnet.core;

public class Debris extends SpaceObject {

    public Debris(
            String id,
            Vector3D position,
            Vector3D velocity,
            double size,
            Status status) {

        super(id, ObjectType.DEBRIS, position, velocity, size, status);
    }
}
