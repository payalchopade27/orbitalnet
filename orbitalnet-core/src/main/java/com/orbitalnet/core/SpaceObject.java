
package com.orbitalnet.core;

public abstract class SpaceObject {

    public enum ObjectType {
        SATELLITE,
        DEBRIS
    }

    public enum Status {
        ACTIVE,
        INACTIVE,
        DESTROYED
    }

    private final String id;
    private final ObjectType type;
    private Vector3D position;
    private Vector3D velocity;
    private final double size;
    private Status status;

    protected SpaceObject(
            String id,
            ObjectType type,
            Vector3D position,
            Vector3D velocity,
            double size,
            Status status) {

        this.id = id;
        this.type = type;
        this.position = position;
        this.velocity = velocity;
        this.size = size;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public ObjectType getType() {
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

    public Status getStatus() {
        return status;
    }

    public void setPosition(Vector3D position) {
        this.position = position;
    }

    public void setVelocity(Vector3D velocity) {
        this.velocity = velocity;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public void advancePosition(double deltaTimeSeconds) {
    if (!isActive()) {
        return;
    }

    Vector3D displacement = velocity.scale(deltaTimeSeconds);
    position = position.add(displacement);
    }
}
