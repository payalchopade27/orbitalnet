
package com.orbitalnet.core;

public class Satellite extends SpaceObject {

    private final String name;
    private final Orbit orbit;

    public Satellite(
            String id,
            String name,
            Vector3D position,
            Vector3D velocity,
            double size,
            Status status,
            Orbit orbit) {

        super(id, ObjectType.SATELLITE, position, velocity, size, status);

        this.name = name;
        this.orbit = orbit;
    }

    public String getName() {
        return name;
    }

    public Orbit getOrbit() {
        return orbit;
    }
}
