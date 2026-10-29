
package com.orbitalnet.core;
import java.util.Objects;
public final class Vector3D {

    private final double x;
    private final double y;
    private final double z;

    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public Vector3D add(Vector3D other) {
        return new Vector3D(
                x + other.x,
                y + other.y,
                z + other.z
        );
    }

    public Vector3D subtract(Vector3D other) {
        return new Vector3D(
                x - other.x,
                y - other.y,
                z - other.z
        );
    }

    public Vector3D scale(double factor) {
        return new Vector3D(
                x * factor,
                y * factor,
                z * factor
        );
    }

    public double dot(Vector3D other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public double magnitude() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double distanceTo(Vector3D other) {
        return this.subtract(other).magnitude();
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Vector3D other)) {
            return false;
        }

        return Double.compare(x, other.x) == 0
                && Double.compare(y, other.y) == 0
                && Double.compare(z, other.z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

}
