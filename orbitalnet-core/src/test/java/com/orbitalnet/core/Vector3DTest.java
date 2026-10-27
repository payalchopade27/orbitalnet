
package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Vector3DTest {

    @Test
    void shouldAddVectors() {
        Vector3D a = new Vector3D(1, 2, 3);
        Vector3D b = new Vector3D(4, 5, 6);

        Vector3D result = a.add(b);

        assertEquals(5, result.getX());
        assertEquals(7, result.getY());
        assertEquals(9, result.getZ());
    }

    @Test
    void shouldSubtractVectors() {
        Vector3D a = new Vector3D(4, 5, 6);
        Vector3D b = new Vector3D(1, 2, 3);

        Vector3D result = a.subtract(b);

        assertEquals(3, result.getX());
        assertEquals(3, result.getY());
        assertEquals(3, result.getZ());
    }

    @Test
    void shouldScaleVector() {
        Vector3D vector = new Vector3D(1, 2, 3);

        Vector3D result = vector.scale(2);

        assertEquals(2, result.getX());
        assertEquals(4, result.getY());
        assertEquals(6, result.getZ());
    }

    @Test
    void shouldCalculateDotProduct() {
        Vector3D a = new Vector3D(1, 2, 3);
        Vector3D b = new Vector3D(4, 5, 6);

        assertEquals(32, a.dot(b));
    }

    @Test
    void shouldCalculateMagnitude() {
        Vector3D vector = new Vector3D(3, 4, 0);

        assertEquals(5.0, vector.magnitude(), 0.000001);
    }

    @Test
    void shouldCalculateDistance() {
        Vector3D a = new Vector3D(1, 2, 3);
        Vector3D b = new Vector3D(4, 6, 3);

        assertEquals(5.0, a.distanceTo(b), 0.000001);
    }


    @Test
    void vectorsWithSameCoordinatesShouldBeEqual() {
        Vector3D first = new Vector3D(1, 2, 3);
        Vector3D second = new Vector3D(1, 2, 3);

        assertEquals(first, second);
    }

    @Test
    void equalVectorsShouldHaveSameHashCode() {
        Vector3D first = new Vector3D(1, 2, 3);
        Vector3D second = new Vector3D(1, 2, 3);

        assertEquals(first.hashCode(), second.hashCode());
    }

}
