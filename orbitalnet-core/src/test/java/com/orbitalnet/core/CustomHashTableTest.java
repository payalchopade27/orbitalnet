
package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomHashTableTest {

    private Satellite createSatellite(String id, double x) {
        return new Satellite(
                id,
                "Test Satellite",
                new Vector3D(x, 0, 0),
                new Vector3D(1, 0, 0),
                1.0,
                SpaceObject.Status.ACTIVE,
                null
        );
    }

    @Test
    void shouldInsertAndRetrieveObject() {
        CustomHashTable table = new CustomHashTable();
        Satellite satellite = createSatellite("SAT-1", 10);

        table.put(satellite);

        assertSame(satellite, table.get("SAT-1"));
        assertEquals(1, table.size());
    }

    @Test
    void shouldReturnNullForMissingId() {
        CustomHashTable table = new CustomHashTable();

        assertNull(table.get("UNKNOWN"));
        assertFalse(table.containsKey("UNKNOWN"));
    }

    @Test
    void shouldHandleCollisions() {
        CustomHashTable table = new CustomHashTable();

        // "SAT-1" and "SAT-17" have the same bucket index
        Satellite first = createSatellite("SAT-1", 10);
        Satellite second = createSatellite("SAT-17", 20);

        table.put(first);
        table.put(second);

        assertSame(first, table.get("SAT-1"));
        assertSame(second, table.get("SAT-17"));
        assertEquals(2, table.size());
    }

    @Test
    void shouldReplaceObjectWithSameId() {
        CustomHashTable table = new CustomHashTable();

        Satellite original = createSatellite("SAT-1", 10);
        Satellite replacement = createSatellite("SAT-1", 50);

        table.put(original);
        table.put(replacement);

        assertSame(replacement, table.get("SAT-1"));
        assertEquals(1, table.size());
        assertEquals(50, table.get("SAT-1").getPosition().getX());
    }

    @Test
    void shouldRejectNullObject() {
        CustomHashTable table = new CustomHashTable();

        assertThrows(
                IllegalArgumentException.class,
                () -> table.put(null)
        );
    }

    @Test
    void shouldRejectNullOrBlankId() {
        CustomHashTable table = new CustomHashTable();

        assertThrows(
                IllegalArgumentException.class,
                () -> table.get(null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> table.get(" ")
        );
    }
}
