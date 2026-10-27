
package com.orbitalnet.core;

public class CustomHashTable {

    private static final int DEFAULT_CAPACITY = 16;

    private Node[] buckets;
    private int size;

    private static class Node {
        private SpaceObject object;
        private Node next;

        private Node(SpaceObject object, Node next) {
            this.object = object;
            this.next = next;
        }
    }

    public CustomHashTable() {
        buckets = new Node[DEFAULT_CAPACITY];
        size = 0;
    }

    private int getBucketIndex(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID cannot be null or blank");
        }

        return Math.floorMod(id.hashCode(), buckets.length);
    }

    public void put(SpaceObject object) {
        if (object == null) {
            throw new IllegalArgumentException("Object cannot be null");
        }

        int index = getBucketIndex(object.getId());
        Node current = buckets[index];

        while (current != null) {
            if (current.object.getId().equals(object.getId())) {
                current.object = object;
                return;
            }

            current = current.next;
        }

        buckets[index] = new Node(object, buckets[index]);
        size++;
    }

    public SpaceObject get(String id) {
        int index = getBucketIndex(id);
        Node current = buckets[index];

        while (current != null) {
            if (current.object.getId().equals(id)) {
                return current.object;
            }

            current = current.next;
        }

        return null;
    }

    public boolean containsKey(String id) {
        return get(id) != null;
    }

    public int size() {
        return size;
    }
}
