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

    @Test
    void shouldHandleNegativeCoordinatesInAllOperations() {
        Vector3D a = new Vector3D(-3.0, 4.0, -5.0);
        Vector3D b = new Vector3D(1.0, -2.0, -3.0);

        Vector3D sum = a.add(b);
        assertEquals(-2.0, sum.getX(), 0.000001);
        assertEquals(2.0, sum.getY(), 0.000001);
        assertEquals(-8.0, sum.getZ(), 0.000001);

        Vector3D diff = a.subtract(b);
        assertEquals(-4.0, diff.getX(), 0.000001);
        assertEquals(6.0, diff.getY(), 0.000001);
        assertEquals(-2.0, diff.getZ(), 0.000001);

        Vector3D scaled = a.scale(-2.0);
        assertEquals(6.0, scaled.getX(), 0.000001);
        assertEquals(-8.0, scaled.getY(), 0.000001);
        assertEquals(10.0, scaled.getZ(), 0.000001);

        assertEquals(4.0, a.dot(b), 0.000001);

        Vector3D c = new Vector3D(-3.0, -4.0, 0.0);
        assertEquals(5.0, c.magnitude(), 0.000001);

        Vector3D p1 = new Vector3D(-1.0, -2.0, -3.0);
        Vector3D p2 = new Vector3D(2.0, 2.0, -3.0);
        assertEquals(5.0, p1.distanceTo(p2), 0.000001);
    }

    @Test
    void distanceBetweenIdenticalVectorsShouldBeZero() {
        Vector3D zero1 = new Vector3D(0.0, 0.0, 0.0);
        Vector3D zero2 = new Vector3D(0.0, 0.0, 0.0);
        assertEquals(0.0, zero1.distanceTo(zero2), 0.000001);
        assertEquals(0.0, zero1.magnitude(), 0.000001);

        Vector3D v = new Vector3D(-12.5, 45.0, -99.75);
        assertEquals(0.0, v.distanceTo(v), 0.000001);

        Vector3D identicalV = new Vector3D(-12.5, 45.0, -99.75);
        assertEquals(0.0, v.distanceTo(identicalV), 0.000001);
    }

    @Test
    void operationsShouldNotMutateInputVectors() {
        Vector3D a = new Vector3D(10.0, 20.0, 30.0);
        Vector3D b = new Vector3D(1.0, 2.0, 3.0);

        Vector3D sum = a.add(b);
        Vector3D diff = a.subtract(b);
        Vector3D scaled = a.scale(4.0);

        // a remains unmutated
        assertEquals(10.0, a.getX());
        assertEquals(20.0, a.getY());
        assertEquals(30.0, a.getZ());
        assertEquals(new Vector3D(10.0, 20.0, 30.0), a);

        // b remains unmutated
        assertEquals(1.0, b.getX());
        assertEquals(2.0, b.getY());
        assertEquals(3.0, b.getZ());
        assertEquals(new Vector3D(1.0, 2.0, 3.0), b);

        // Results are independent new instances
        assertNotSame(a, sum);
        assertNotSame(b, sum);
        assertNotSame(a, diff);
        assertNotSame(a, scaled);
    }
}
