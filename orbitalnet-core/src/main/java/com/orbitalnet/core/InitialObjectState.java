
package com.orbitalnet.core;

import java.util.Objects;

public final class InitialObjectState {

    private final String id;
    private final SpaceObject.ObjectType type;
    private final Vector3D position;
    private final Vector3D velocity;
    private final double size;
    private final SpaceObject.Status status;

    // Subtype-specific information
    private final String name;
    private final Orbit orbit;

    public InitialObjectState(
            String id,
            SpaceObject.ObjectType type,
            Vector3D position,
            Vector3D velocity,
            double size,
            SpaceObject.Status status,
            String name,
            Orbit orbit) {

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Object ID cannot be blank.");
        }

        this.id = id;
        this.type = Objects.requireNonNull(type, "Object type cannot be null.");
        this.position = Objects.requireNonNull(position, "Position cannot be null.");
        this.velocity = Objects.requireNonNull(velocity, "Velocity cannot be null.");
        this.status = Objects.requireNonNull(status, "Status cannot be null.");

        if (!Double.isFinite(size) || size <= 0) {
            throw new IllegalArgumentException(
                    "Object size must be finite and greater than zero.");
        }

        if (type == SpaceObject.ObjectType.SATELLITE) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                        "Satellite name cannot be blank.");
            }
        } else if (type == SpaceObject.ObjectType.DEBRIS) {
            if (name != null || orbit != null) {
                throw new IllegalArgumentException(
                        "Debris cannot have satellite-specific information.");
            }
        }

        this.size = size;
        this.name = name;
        this.orbit = orbit;
    }

    public String getId() {
        return id;
    }

    public SpaceObject.ObjectType getType() {
        return type;
    }

    public Vector3D getPosition() {
        return position;
    }

    public Vector3D getVelocity() {
        return velocity;
    }

    public double getSize() {
        return size;
    }

    public SpaceObject.Status getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public Orbit getOrbit() {
        return orbit;
    }


    public SpaceObject toSpaceObject() {
        if (type == SpaceObject.ObjectType.SATELLITE) {
            return new Satellite(
                    id,
                    name,
                    position,
                    velocity,
                    size,
                    status,
                    orbit
            );
        }

        if (type == SpaceObject.ObjectType.DEBRIS) {
            return new Debris(
                    id,
                    position,
                    velocity,
                    size,
                    status
            );
        }

        throw new IllegalStateException(
                "Unsupported object type: " + type);
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof InitialObjectState other)) {
            return false;
        }

        return Double.compare(size, other.size) == 0
                && id.equals(other.id)
                && type == other.type
                && position.equals(other.position)
                && velocity.equals(other.velocity)
                && status == other.status
                && Objects.equals(name, other.name)
                && Objects.equals(orbit, other.orbit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id, type, position, velocity, size, status, name, orbit
        );
    }

}
