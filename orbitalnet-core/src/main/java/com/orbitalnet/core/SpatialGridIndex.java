package com.orbitalnet.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class SpatialGridIndex {

    public static final double CELL_SIZE_KM = 100.0;

    private final double cellSizeKm;

    private static final int GRID_CAPACITY = 256;

    private final CellEntry[] cellBuckets;
    private static final long MAX_CELLS_PER_OBJECT = 10_000;

    private final List<SpaceObject> overflowObjects = new ArrayList<>();
    private final List<SpaceObject> indexedObjects = new ArrayList<>();
    private final List<CellEntry> allCells = new ArrayList<>();

    private static class CellEntry {
        private final CellCoordinate coordinate;
        private ObjectEntry objects;
        private CellEntry next;

        private CellEntry(CellCoordinate coordinate, CellEntry next) {
            this.coordinate = coordinate;
            this.next = next;
        }
    }

    private static class ObjectEntry {
        private final SpaceObject object;
        private ObjectEntry next;

        private ObjectEntry(SpaceObject object, ObjectEntry next) {
            this.object = object;
            this.next = next;
        }
    }

    private int getBucketIndex(CellCoordinate coordinate) {
        return Math.floorMod(coordinate.hashCode(), cellBuckets.length);
    }

    private CellEntry findOrCreateCell(CellCoordinate coordinate) {
        int index = getBucketIndex(coordinate);
        CellEntry current = cellBuckets[index];

        while (current != null) {
            if (current.coordinate.equals(coordinate)) {
                return current;
            }
            current = current.next;
        }

        CellEntry newCell = new CellEntry(coordinate, cellBuckets[index]);
        cellBuckets[index] = newCell;
        allCells.add(newCell);
        return newCell;
    }

    public void addToCell(CellCoordinate coordinate, SpaceObject object) {
        Objects.requireNonNull(coordinate, "Cell coordinate cannot be null.");
        Objects.requireNonNull(object, "Space object cannot be null.");

        if (!object.isActive()) {
            return;
        }

        CellEntry cell = findOrCreateCell(coordinate);
        ObjectEntry current = cell.objects;

        while (current != null) {
            if (current.object.getId().equals(object.getId())) {
                return;
            }
            current = current.next;
        }

        cell.objects = new ObjectEntry(object, cell.objects);
    }

    public List<SpaceObject> getObjectsInCell(CellCoordinate coordinate) {
        Objects.requireNonNull(coordinate, "Cell coordinate cannot be null.");

        int index = getBucketIndex(coordinate);
        CellEntry cell = cellBuckets[index];

        while (cell != null && !cell.coordinate.equals(coordinate)) {
            cell = cell.next;
        }

        List<SpaceObject> result = new ArrayList<>();

        if (cell == null) {
            return result;
        }

        ObjectEntry current = cell.objects;

        while (current != null) {
            if (current.object.isActive()) {
                result.add(current.object);
            }
            current = current.next;
        }

        return result;
    }

    public SpatialGridIndex() {
        this(CELL_SIZE_KM);
    }

    public SpatialGridIndex(double cellSizeKm) {
        if (!Double.isFinite(cellSizeKm) || cellSizeKm <= 0.0) {
            throw new IllegalArgumentException(
                    "Cell size must be finite and greater than zero.");
        }

        this.cellSizeKm = cellSizeKm;
        this.cellBuckets = new CellEntry[GRID_CAPACITY];
    }

    public double getCellSizeKm() {
        return cellSizeKm;
    }

    public CellCoordinate getCellCoordinate(Vector3D position) {
        Objects.requireNonNull(position, "Position cannot be null.");

        return new CellCoordinate(
                toCellIndex(position.getX()),
                toCellIndex(position.getY()),
                toCellIndex(position.getZ())
        );
    }

    private int toCellIndex(double coordinateKm) {
        if (!Double.isFinite(coordinateKm)) {
            throw new IllegalArgumentException(
                    "Position coordinates must be finite.");
        }

        double index = Math.floor(coordinateKm / cellSizeKm);

        if (index < Integer.MIN_VALUE || index > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Position exceeds supported grid coordinates.");
        }

        return (int) index;
    }

    public static final class CellCoordinate {

        private final int x;
        private final int y;
        private final int z;

        public CellCoordinate(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof CellCoordinate)) {
                return false;
            }

            CellCoordinate cell = (CellCoordinate) other;

            return x == cell.x && y == cell.y && z == cell.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }

    public static final class CandidatePair {
        private final SpaceObject objectA;
        private final SpaceObject objectB;

        public CandidatePair(SpaceObject objectA, SpaceObject objectB) {
            Objects.requireNonNull(objectA, "Object A cannot be null.");
            Objects.requireNonNull(objectB, "Object B cannot be null.");

            if (objectA.getId().equals(objectB.getId())) {
                throw new IllegalArgumentException(
                        "Candidate pair must involve two different objects.");
            }

            if (objectA.getId().compareTo(objectB.getId()) < 0) {
                this.objectA = objectA;
                this.objectB = objectB;
            } else {
                this.objectA = objectB;
                this.objectB = objectA;
            }
        }

        public SpaceObject getObjectA() {
            return objectA;
        }

        public SpaceObject getObjectB() {
            return objectB;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof CandidatePair)) {
                return false;
            }

            CandidatePair that = (CandidatePair) other;

            return objectA.getId().equals(that.objectA.getId())
                    && objectB.getId().equals(that.objectB.getId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(objectA.getId(), objectB.getId());
        }

        @Override
        public String toString() {
            return "CandidatePair[" + objectA.getId() + ", " + objectB.getId() + "]";
        }
    }

    public List<CandidatePair> generateCandidatePairs() {
        Set<CandidatePair> pairs = new LinkedHashSet<>();

        // Compare objects that share at least one grid cell.
        for (CellEntry cell : allCells) {
            for (ObjectEntry first = cell.objects;
                    first != null; first = first.next) {

                if (!first.object.isActive()) {
                    continue;
                }

                for (ObjectEntry second = first.next;
                        second != null; second = second.next) {

                    if (!second.object.isActive()) {
                        continue;
                    }

                    addCandidatePair(pairs, first.object, second.object);
                }
            }
        }

        // Overflow objects must be compared with every active object.
        for (SpaceObject overflow : overflowObjects) {
            if (!overflow.isActive()) {
                continue;
            }

            for (SpaceObject other : indexedObjects) {
                if (!other.isActive() || overflow.getId().equals(other.getId())) {
                    continue;
                }

                addCandidatePair(pairs, overflow, other);
            }
        }

        return new ArrayList<>(pairs);
    }

    private void addCandidatePair(
            Set<CandidatePair> pairs,
            SpaceObject first,
            SpaceObject second) {

        if (first == null || second == null) {
            return;
        }

        if (first.getId().equals(second.getId())) {
            return;
        }

        if (!first.isActive() || !second.isActive()) {
            return;
        }

        pairs.add(new CandidatePair(first, second));
    }

    public void buildIndex(List<SpaceObject> objects) {
        Objects.requireNonNull(objects, "Objects list cannot be null.");

        // Clear the previous index.
        for (int i = 0; i < cellBuckets.length; i++) {
            cellBuckets[i] = null;
        }
        allCells.clear();
        overflowObjects.clear();
        indexedObjects.clear();

        for (SpaceObject object : objects) {
            Objects.requireNonNull(object, "Object list cannot contain null.");

            if (!object.isActive()) {
                continue;
            }

            indexedObjects.add(object);

            Vector3D start = object.getPosition();
            Vector3D end = start.add(
                    object.getVelocity().scale(
                            SimulationConstants.SIMULATION_HORIZON_SECONDS));

            double minX = Math.min(start.getX(), end.getX()) - EncounterDetectionService.DETECTION_DISTANCE_KM;
            double minY = Math.min(start.getY(), end.getY()) - EncounterDetectionService.DETECTION_DISTANCE_KM;
            double minZ = Math.min(start.getZ(), end.getZ()) - EncounterDetectionService.DETECTION_DISTANCE_KM;

            double maxX = Math.max(start.getX(), end.getX()) + EncounterDetectionService.DETECTION_DISTANCE_KM;
            double maxY = Math.max(start.getY(), end.getY()) + EncounterDetectionService.DETECTION_DISTANCE_KM;
            double maxZ = Math.max(start.getZ(), end.getZ()) + EncounterDetectionService.DETECTION_DISTANCE_KM;

            try {
                int lowX = toCellIndex(minX);
                int lowY = toCellIndex(minY);
                int lowZ = toCellIndex(minZ);

                int highX = toCellIndex(maxX);
                int highY = toCellIndex(maxY);
                int highZ = toCellIndex(maxZ);

                long width = (long) highX - lowX + 1;
                long height = (long) highY - lowY + 1;
                long depth = (long) highZ - lowZ + 1;

                // Avoid overflow and excessive cell insertion.
                if (width <= 0 || height <= 0 || depth <= 0
                        || width > MAX_CELLS_PER_OBJECT
                        || height > MAX_CELLS_PER_OBJECT
                        || depth > MAX_CELLS_PER_OBJECT
                        || width * height > MAX_CELLS_PER_OBJECT
                        || width * height * depth > MAX_CELLS_PER_OBJECT) {
                    overflowObjects.add(object);
                    continue;
                }

                for (long x = lowX; x <= highX; x++) {
                    for (long y = lowY; y <= highY; y++) {
                        for (long z = lowZ; z <= highZ; z++) {
                            CellCoordinate cell =
                                    new CellCoordinate((int) x, (int) y, (int) z);
                            addToCell(cell, object);
                        }
                    }
                }
            } catch (IllegalArgumentException exception) {
                // Coordinates outside the supported grid use the safe fallback.
                overflowObjects.add(object);
            }
        }
    }

    public List<SpaceObject> getOverflowObjects() {
        return Collections.unmodifiableList(overflowObjects);
    }

    public List<SpaceObject> getIndexedObjects() {
        return Collections.unmodifiableList(indexedObjects);
    }

}
