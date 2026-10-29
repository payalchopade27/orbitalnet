package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SpatialGridIndexTest {

    private final SpatialGridIndex grid = new SpatialGridIndex();

    @Test
    void mapsPositiveCoordinatesToCorrectCell() {
        SpatialGridIndex.CellCoordinate cell =
                grid.getCellCoordinate(new Vector3D(25, 40, 90));

        assertEquals(0, cell.getX());
        assertEquals(0, cell.getY());
        assertEquals(0, cell.getZ());
    }

    @Test
    void handlesNegativeCoordinatesUsingFloor() {
        SpatialGridIndex.CellCoordinate cell =
                grid.getCellCoordinate(new Vector3D(-1, -101, -200));

        assertEquals(-1, cell.getX());
        assertEquals(-2, cell.getY());
        assertEquals(-2, cell.getZ());
    }

    @Test
    void handlesExactCellBoundaries() {
        SpatialGridIndex.CellCoordinate cell =
                grid.getCellCoordinate(new Vector3D(100, -100, 200));

        assertEquals(1, cell.getX());
        assertEquals(-1, cell.getY());
        assertEquals(2, cell.getZ());
    }

    @Test
    void supportsCustomCellSize() {
        SpatialGridIndex customGrid = new SpatialGridIndex(50);

        SpatialGridIndex.CellCoordinate cell =
                customGrid.getCellCoordinate(new Vector3D(75, 0, -75));

        assertEquals(1, cell.getX());
        assertEquals(0, cell.getY());
        assertEquals(-2, cell.getZ());
    }

    @Test
    void rejectsInvalidCellSize() {
        assertThrows(IllegalArgumentException.class,
                () -> new SpatialGridIndex(0));

        assertThrows(IllegalArgumentException.class,
                () -> new SpatialGridIndex(-10));

        assertThrows(IllegalArgumentException.class,
                () -> new SpatialGridIndex(Double.NaN));
    }

    @Test
    void rejectsNullPosition() {
        assertThrows(NullPointerException.class,
                () -> grid.getCellCoordinate(null));
    }

    @Test
    void rejectsNonFinitePositionCoordinates() {
        assertThrows(IllegalArgumentException.class,
                () -> grid.getCellCoordinate(
                        new Vector3D(Double.NaN, 0, 0)));
    }

    @Test
    void storesAndRetrievesObjectsInCell() {
        SpatialGridIndex.CellCoordinate cell =
                grid.getCellCoordinate(new Vector3D(10, 20, 30));

        SpaceObject satellite = new Satellite(
                "SAT-1", "Test Satellite",
                new Vector3D(10, 20, 30),
                new Vector3D(0, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null
        );

        grid.addToCell(cell, satellite);

        assertEquals(1, grid.getObjectsInCell(cell).size());
        assertEquals("SAT-1", grid.getObjectsInCell(cell).get(0).getId());
    }

    @Test
    void doesNotAddSameObjectTwiceToOneCell() {
        SpatialGridIndex.CellCoordinate cell =
                grid.getCellCoordinate(new Vector3D(10, 20, 30));

        SpaceObject satellite = new Satellite(
                "SAT-1", "Test Satellite",
                new Vector3D(10, 20, 30),
                new Vector3D(0, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null
        );

        grid.addToCell(cell, satellite);
        grid.addToCell(cell, satellite);

        assertEquals(1, grid.getObjectsInCell(cell).size());
    }

    @Test
    void ignoresInactiveObjects() {
        SpatialGridIndex.CellCoordinate cell =
                grid.getCellCoordinate(new Vector3D(10, 20, 30));

        SpaceObject debris = new Debris(
                "DEB-1",
                new Vector3D(10, 20, 30),
                new Vector3D(0, 0, 0),
                1.0,
                SpaceObject.Status.INACTIVE
        );

        grid.addToCell(cell, debris);

        assertTrue(grid.getObjectsInCell(cell).isEmpty());
    }

    @Test
    void returnsEmptyListForUnknownCell() {
        SpatialGridIndex.CellCoordinate cell =
                new SpatialGridIndex.CellCoordinate(99, 99, 99);

        assertTrue(grid.getObjectsInCell(cell).isEmpty());
    }

    @Test
    void indexesCellsAlongPredictedPath() {
        SpatialGridIndex pathGrid = new SpatialGridIndex();

        SpaceObject satellite = new Satellite(
                "SAT-PATH", "Path Test",
                new Vector3D(0, 0, 0),
                new Vector3D(1, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null
        );

        pathGrid.buildIndex(java.util.List.of(satellite));

        SpatialGridIndex.CellCoordinate startCell =
                pathGrid.getCellCoordinate(new Vector3D(0, 0, 0));

        SpatialGridIndex.CellCoordinate middleCell =
                pathGrid.getCellCoordinate(new Vector3D(1800, 0, 0));

        assertTrue(pathGrid.getObjectsInCell(startCell).stream()
                .anyMatch(o -> o.getId().equals("SAT-PATH")));

        assertTrue(pathGrid.getObjectsInCell(middleCell).stream()
                .anyMatch(o -> o.getId().equals("SAT-PATH")));
    }

    @Test
    void excludesInactiveObjectsFromBuiltIndex() {
        SpatialGridIndex pathGrid = new SpatialGridIndex();

        SpaceObject debris = new Debris(
                "DEB-INACTIVE",
                new Vector3D(0, 0, 0),
                new Vector3D(1, 0, 0),
                1.0,
                SpaceObject.Status.INACTIVE
        );

        pathGrid.buildIndex(java.util.List.of(debris));

        assertTrue(pathGrid.getIndexedObjects().isEmpty());
        assertTrue(pathGrid.getOverflowObjects().isEmpty());
    }

    @Test
    void sendsVeryLargeSweptRegionToOverflowFallback() {
        SpatialGridIndex pathGrid = new SpatialGridIndex();

        SpaceObject satellite = new Satellite(
                "SAT-FAST", "Fast Satellite",
                new Vector3D(0, 0, 0),
                new Vector3D(100, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null
        );

        pathGrid.buildIndex(java.util.List.of(satellite));

        assertEquals(1, pathGrid.getIndexedObjects().size());
        assertEquals(1, pathGrid.getOverflowObjects().size());
        assertEquals("SAT-FAST", pathGrid.getOverflowObjects().get(0).getId());
    }

    @Test
    void emptyInputAndSingleActiveObjectProduceNoPairs() {
        SpatialGridIndex gridIndex = new SpatialGridIndex();

        gridIndex.buildIndex(List.of());
        assertTrue(gridIndex.generateCandidatePairs().isEmpty());

        SpaceObject satellite = new Satellite(
                "SAT-1", "Solo Satellite",
                new Vector3D(0, 0, 0),
                new Vector3D(1, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null
        );

        gridIndex.buildIndex(List.of(satellite));
        assertTrue(gridIndex.generateCandidatePairs().isEmpty());
    }

    @Test
    void twoObjectsSharingMultipleCellsProduceExactlyOnePair() {
        SpatialGridIndex gridIndex = new SpatialGridIndex();

        SpaceObject sat1 = new Satellite(
                "SAT-1", "Alpha",
                new Vector3D(0, 0, 0),
                new Vector3D(1, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null
        );

        SpaceObject sat2 = new Satellite(
                "SAT-2", "Beta",
                new Vector3D(50, 0, 0),
                new Vector3D(1, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null
        );

        gridIndex.buildIndex(List.of(sat1, sat2));
        List<SpatialGridIndex.CandidatePair> pairs = gridIndex.generateCandidatePairs();

        assertEquals(1, pairs.size());
        SpatialGridIndex.CandidatePair pair = pairs.get(0);
        assertEquals("SAT-1", pair.getObjectA().getId());
        assertEquals("SAT-2", pair.getObjectB().getId());
    }

    @Test
    void candidatePairGenerationExcludesInactiveObjects() {
        SpatialGridIndex gridIndex = new SpatialGridIndex();

        SpaceObject activeSat = new Satellite(
                "SAT-ACTIVE", "Active Satellite",
                new Vector3D(0, 0, 0),
                new Vector3D(0, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null
        );

        SpaceObject inactiveDebris = new Debris(
                "DEB-INACTIVE",
                new Vector3D(10, 10, 10),
                new Vector3D(0, 0, 0),
                1.0,
                SpaceObject.Status.INACTIVE
        );

        gridIndex.buildIndex(List.of(activeSat, inactiveDebris));
        assertTrue(gridIndex.generateCandidatePairs().isEmpty());
    }

    @Test
    void pairsOverflowObjectsWithOtherActiveIndexedObjectsWithoutDuplicatesOrSelfPairs() {
        SpatialGridIndex gridIndex = new SpatialGridIndex();

        // Two overflow objects (velocity 100 km/s -> swept cells exceed threshold)
        SpaceObject fastSat1 = new Satellite(
                "SAT-FAST-1", "Fast One",
                new Vector3D(0, 0, 0),
                new Vector3D(100, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null
        );

        SpaceObject fastSat2 = new Satellite(
                "SAT-FAST-2", "Fast Two",
                new Vector3D(500, 500, 500),
                new Vector3D(0, 100, 0),
                2.0, SpaceObject.Status.ACTIVE, null
        );

        // Normal object in a separate cell
        SpaceObject normalSat = new Satellite(
                "SAT-NORMAL", "Normal One",
                new Vector3D(10000, 10000, 10000),
                new Vector3D(0, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null
        );

        gridIndex.buildIndex(List.of(fastSat1, fastSat2, normalSat));
        List<SpatialGridIndex.CandidatePair> pairs = gridIndex.generateCandidatePairs();

        // Expected pairs: (fastSat1, fastSat2), (fastSat1, normalSat), (fastSat2, normalSat) = 3 pairs
        assertEquals(3, pairs.size());

        for (SpatialGridIndex.CandidatePair pair : pairs) {
            assertNotEquals(pair.getObjectA().getId(), pair.getObjectB().getId());
            assertTrue(pair.getObjectA().getId().compareTo(pair.getObjectB().getId()) < 0);
        }

        assertTrue(pairs.contains(new SpatialGridIndex.CandidatePair(fastSat1, fastSat2)));
        assertTrue(pairs.contains(new SpatialGridIndex.CandidatePair(fastSat1, normalSat)));
        assertTrue(pairs.contains(new SpatialGridIndex.CandidatePair(fastSat2, normalSat)));
    }

    @Test
    void distinctPairsWithIdsContainingSeparatorsDoNotCollapse() {
        SpatialGridIndex gridIndex = new SpatialGridIndex();

        // Pair 1: IDs "A|B" and "C" in cell around (0,0,0)
        SpaceObject obj1 = new Satellite(
                "A|B", "Obj 1",
                new Vector3D(0, 0, 0),
                new Vector3D(0, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null
        );
        SpaceObject obj2 = new Satellite(
                "C", "Obj 2",
                new Vector3D(10, 0, 0),
                new Vector3D(0, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null
        );

        // Pair 2: IDs "A" and "B|C" in cell around (10000,10000,10000)
        SpaceObject obj3 = new Satellite(
                "A", "Obj 3",
                new Vector3D(10000, 10000, 10000),
                new Vector3D(0, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null
        );
        SpaceObject obj4 = new Satellite(
                "B|C", "Obj 4",
                new Vector3D(10010, 10000, 10000),
                new Vector3D(0, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null
        );

        gridIndex.buildIndex(List.of(obj1, obj2, obj3, obj4));
        List<SpatialGridIndex.CandidatePair> pairs = gridIndex.generateCandidatePairs();

        assertEquals(2, pairs.size());
        assertTrue(pairs.contains(new SpatialGridIndex.CandidatePair(obj1, obj2)));
        assertTrue(pairs.contains(new SpatialGridIndex.CandidatePair(obj3, obj4)));
    }

    @Test
    void rebuildingIndexClearsPreviousObjectsAndPairs() {
        SpatialGridIndex gridIndex = new SpatialGridIndex();

        SpaceObject sat1 = new Satellite(
                "SAT-1", "Alpha",
                new Vector3D(0, 0, 0),
                new Vector3D(0, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null
        );
        SpaceObject sat2 = new Satellite(
                "SAT-2", "Beta",
                new Vector3D(10, 0, 0),
                new Vector3D(0, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null
        );

        gridIndex.buildIndex(List.of(sat1, sat2));
        assertEquals(1, gridIndex.generateCandidatePairs().size());

        // Rebuild with completely new single object
        SpaceObject sat3 = new Satellite(
                "SAT-3", "Gamma",
                new Vector3D(5000, 5000, 5000),
                new Vector3D(0, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null
        );

        gridIndex.buildIndex(List.of(sat3));

        assertTrue(gridIndex.generateCandidatePairs().isEmpty());
        assertEquals(1, gridIndex.getIndexedObjects().size());
        assertEquals("SAT-3", gridIndex.getIndexedObjects().get(0).getId());

        // Rebuild with empty list
        gridIndex.buildIndex(List.of());
        assertTrue(gridIndex.generateCandidatePairs().isEmpty());
        assertTrue(gridIndex.getIndexedObjects().isEmpty());
        assertTrue(gridIndex.getOverflowObjects().isEmpty());
    }

    @Test
    void candidatePairCanonicalizesOrderAndEnforcesEqualityContract() {
        SpaceObject satA = new Satellite(
                "AAA", "Alpha",
                new Vector3D(0, 0, 0),
                new Vector3D(0, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null
        );
        SpaceObject satB = new Satellite(
                "BBB", "Beta",
                new Vector3D(10, 0, 0),
                new Vector3D(0, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null
        );

        SpatialGridIndex.CandidatePair pairForward = new SpatialGridIndex.CandidatePair(satA, satB);
        SpatialGridIndex.CandidatePair pairReverse = new SpatialGridIndex.CandidatePair(satB, satA);

        assertEquals("AAA", pairForward.getObjectA().getId());
        assertEquals("BBB", pairForward.getObjectB().getId());
        assertEquals("AAA", pairReverse.getObjectA().getId());
        assertEquals("BBB", pairReverse.getObjectB().getId());

        assertEquals(pairForward, pairReverse);
        assertEquals(pairForward.hashCode(), pairReverse.hashCode());

        assertThrows(IllegalArgumentException.class,
                () -> new SpatialGridIndex.CandidatePair(satA, satA));
        assertThrows(NullPointerException.class,
                () -> new SpatialGridIndex.CandidatePair(null, satA));
        assertThrows(NullPointerException.class,
                () -> new SpatialGridIndex.CandidatePair(satA, null));
    }

    // --- Naive Reference Validation Tests ---

    private Set<SpatialGridIndex.CandidatePair> computeNaiveReferencePairs(List<SpaceObject> objects) {
        Set<SpatialGridIndex.CandidatePair> qualifyingPairs = new LinkedHashSet<>();

        for (int i = 0; i < objects.size(); i++) {
            SpaceObject a = objects.get(i);
            if (!a.isActive()) {
                continue;
            }

            for (int j = i + 1; j < objects.size(); j++) {
                SpaceObject b = objects.get(j);
                if (!b.isActive()) {
                    continue;
                }

                ClosestApproach ca = new ClosestApproach(
                        a.getPosition(), a.getVelocity(),
                        b.getPosition(), b.getVelocity()
                );

                if (ca.getDistanceKm() <= EncounterDetectionService.DETECTION_DISTANCE_KM) {
                    qualifyingPairs.add(new SpatialGridIndex.CandidatePair(a, b));
                }
            }
        }

        return qualifyingPairs;
    }

    private void assertGridContainsAllNaiveQualifyingPairs(
            SpatialGridIndex gridIndex,
            List<SpaceObject> objects) {

        gridIndex.buildIndex(objects);
        List<SpatialGridIndex.CandidatePair> candidatePairsList = gridIndex.generateCandidatePairs();
        Set<SpatialGridIndex.CandidatePair> candidatePairs = new LinkedHashSet<>(candidatePairsList);

        // Verify broad-phase candidate integrity
        assertEquals(candidatePairsList.size(), candidatePairs.size(),
                "Grid returned duplicate candidate pairs.");

        for (SpatialGridIndex.CandidatePair pair : candidatePairsList) {
            assertNotNull(pair.getObjectA());
            assertNotNull(pair.getObjectB());
            assertNotEquals(pair.getObjectA().getId(), pair.getObjectB().getId(),
                    "Grid returned a self-pair.");
            assertTrue(pair.getObjectA().isActive(),
                    "Candidate pair must not contain inactive objectA.");
            assertTrue(pair.getObjectB().isActive(),
                    "Candidate pair must not contain inactive objectB.");
            assertTrue(pair.getObjectA().getId().compareTo(pair.getObjectB().getId()) < 0,
                    "Candidate pair objects must be in canonical order.");
        }

        Set<SpatialGridIndex.CandidatePair> naivePairs = computeNaiveReferencePairs(objects);

        for (SpatialGridIndex.CandidatePair expectedPair : naivePairs) {
            assertTrue(
                    candidatePairs.contains(expectedPair),
                    () -> "Grid candidates missed qualifying encounter pair: "
                            + expectedPair.getObjectA().getId() + " and "
                            + expectedPair.getObjectB().getId()
            );
        }
    }

    @Test
    void validateAgainstNaiveReferenceNegativeCoordinates() {
        SpatialGridIndex gridIndex = new SpatialGridIndex();

        List<SpaceObject> objects = List.of(
                new Satellite("SAT-NEG-1", "Neg 1",
                        new Vector3D(-500, -300, -100),
                        new Vector3D(0.1, -0.05, 0.0),
                        2.0, SpaceObject.Status.ACTIVE, null),
                new Satellite("SAT-NEG-2", "Neg 2",
                        new Vector3D(-460, -290, -90),
                        new Vector3D(-0.05, 0.05, 0.0),
                        2.0, SpaceObject.Status.ACTIVE, null),
                new Debris("DEB-NEG-1",
                        new Vector3D(-1500, -1500, -1500),
                        new Vector3D(0.0, 0.0, 0.0),
                        1.0, SpaceObject.Status.ACTIVE),
                new Debris("DEB-NEG-2",
                        new Vector3D(-1480, -1470, -1490),
                        new Vector3D(-0.01, 0.02, 0.01),
                        1.0, SpaceObject.Status.ACTIVE)
        );

        assertGridContainsAllNaiveQualifyingPairs(gridIndex, objects);
    }

    @Test
    void validateAgainstNaiveReferenceStationaryObjects() {
        SpatialGridIndex gridIndex = new SpatialGridIndex();

        // Pair 1: 50 km apart (qualifying)
        SpaceObject sat1 = new Satellite("SAT-S1", "S1",
                new Vector3D(0, 0, 0), new Vector3D(0, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null);
        SpaceObject sat2 = new Satellite("SAT-S2", "S2",
                new Vector3D(50, 0, 0), new Vector3D(0, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null);

        // Pair 2: 99.9 km apart (qualifying)
        SpaceObject sat3 = new Satellite("SAT-S3", "S3",
                new Vector3D(1000, 1000, 1000), new Vector3D(0, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null);
        SpaceObject sat4 = new Satellite("SAT-S4", "S4",
                new Vector3D(1099.9, 1000, 1000), new Vector3D(0, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null);

        // Pair 3: exactly 100.0 km apart (boundary, qualifying)
        SpaceObject sat5 = new Satellite("SAT-S5", "S5",
                new Vector3D(3000, 3000, 3000), new Vector3D(0, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null);
        SpaceObject sat6 = new Satellite("SAT-S6", "S6",
                new Vector3D(3100.0, 3000, 3000), new Vector3D(0, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null);

        // Pair 4: 100.1 km apart (non-qualifying)
        SpaceObject sat7 = new Satellite("SAT-S7", "S7",
                new Vector3D(5000, 5000, 5000), new Vector3D(0, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null);
        SpaceObject sat8 = new Satellite("SAT-S8", "S8",
                new Vector3D(5100.1, 5000, 5000), new Vector3D(0, 0, 0),
                2.0, SpaceObject.Status.ACTIVE, null);

        List<SpaceObject> objects = List.of(sat1, sat2, sat3, sat4, sat5, sat6, sat7, sat8);

        Set<SpatialGridIndex.CandidatePair> naivePairs = computeNaiveReferencePairs(objects);
        assertTrue(naivePairs.contains(new SpatialGridIndex.CandidatePair(sat1, sat2)));
        assertTrue(naivePairs.contains(new SpatialGridIndex.CandidatePair(sat3, sat4)));
        assertTrue(naivePairs.contains(new SpatialGridIndex.CandidatePair(sat5, sat6)));
        assertFalse(naivePairs.contains(new SpatialGridIndex.CandidatePair(sat7, sat8)));

        assertGridContainsAllNaiveQualifyingPairs(gridIndex, objects);
    }

    @Test
    void validateAgainstNaiveReferenceApproachingAndRecedingObjects() {
        SpatialGridIndex gridIndex = new SpatialGridIndex();

        List<SpaceObject> objects = List.of(
                // Head-on collision trajectory (approaching)
                new Satellite("SAT-HEAD-A", "Head A",
                        new Vector3D(-200, 0, 0), new Vector3D(1.0, 0, 0),
                        2.0, SpaceObject.Status.ACTIVE, null),
                new Satellite("SAT-HEAD-B", "Head B",
                        new Vector3D(200, 0, 0), new Vector3D(-1.0, 0, 0),
                        2.0, SpaceObject.Status.ACTIVE, null),

                // Oblique near-miss trajectory
                new Satellite("SAT-OBL-A", "Obl A",
                        new Vector3D(1000, 0, 0), new Vector3D(0.5, 0.2, 0),
                        2.0, SpaceObject.Status.ACTIVE, null),
                new Debris("DEB-OBL-B",
                        new Vector3D(1000, 500, 0), new Vector3D(0.5, -0.2, 0),
                        1.0, SpaceObject.Status.ACTIVE),

                // Diverging trajectory starting within threshold (t=0 separation is 80 km)
                new Satellite("SAT-DIV-A", "Div A",
                        new Vector3D(3000, 0, 0), new Vector3D(-0.5, 0, 0),
                        2.0, SpaceObject.Status.ACTIVE, null),
                new Satellite("SAT-DIV-B", "Div B",
                        new Vector3D(3080, 0, 0), new Vector3D(0.5, 0, 0),
                        2.0, SpaceObject.Status.ACTIVE, null),

                // Diverging trajectory starting outside threshold (separation 250 km)
                new Satellite("SAT-FAR-A", "Far A",
                        new Vector3D(5000, 0, 0), new Vector3D(-0.5, 0, 0),
                        2.0, SpaceObject.Status.ACTIVE, null),
                new Satellite("SAT-FAR-B", "Far B",
                        new Vector3D(5250, 0, 0), new Vector3D(0.5, 0, 0),
                        2.0, SpaceObject.Status.ACTIVE, null)
        );

        assertGridContainsAllNaiveQualifyingPairs(gridIndex, objects);
    }

    @Test
    void validateAgainstNaiveReferenceCellBoundaries() {
        SpatialGridIndex gridIndex = new SpatialGridIndex();

        List<SpaceObject> objects = List.of(
                // Across X=0 boundary (cell -1 and cell 0)
                new Satellite("SAT-BND-1", "B1",
                        new Vector3D(-0.05, 10, 10), new Vector3D(0, 0, 0),
                        2.0, SpaceObject.Status.ACTIVE, null),
                new Satellite("SAT-BND-2", "B2",
                        new Vector3D(0.05, 10, 10), new Vector3D(0, 0, 0),
                        2.0, SpaceObject.Status.ACTIVE, null),

                // Across X=100 boundary (cell 0 and cell 1)
                new Satellite("SAT-BND-3", "B3",
                        new Vector3D(99.9, 500, 500), new Vector3D(0, 0, 0),
                        2.0, SpaceObject.Status.ACTIVE, null),
                new Satellite("SAT-BND-4", "B4",
                        new Vector3D(100.1, 500, 500), new Vector3D(0, 0, 0),
                        2.0, SpaceObject.Status.ACTIVE, null),

                // Across multiple cell boundaries simultaneously (X=100, Y=-100, Z=0)
                new Satellite("SAT-BND-5", "B5",
                        new Vector3D(99.9, -99.9, 0.1), new Vector3D(0.01, -0.01, 0.0),
                        2.0, SpaceObject.Status.ACTIVE, null),
                new Satellite("SAT-BND-6", "B6",
                        new Vector3D(100.1, -100.1, -0.1), new Vector3D(-0.01, 0.01, 0.0),
                        2.0, SpaceObject.Status.ACTIVE, null),

                // Exactly on integer boundary
                new Satellite("SAT-BND-7", "B7",
                        new Vector3D(100.0, 100.0, 100.0), new Vector3D(0, 0, 0),
                        2.0, SpaceObject.Status.ACTIVE, null),
                new Satellite("SAT-BND-8", "B8",
                        new Vector3D(100.0, 140.0, 100.0), new Vector3D(0, 0, 0),
                        2.0, SpaceObject.Status.ACTIVE, null)
        );

        assertGridContainsAllNaiveQualifyingPairs(gridIndex, objects);
    }

    @Test
    void validateAgainstNaiveReferenceInactiveObjectsExcluded() {
        SpatialGridIndex gridIndex = new SpatialGridIndex();

        List<SpaceObject> objects = List.of(
                // Active pair close to each other
                new Satellite("SAT-ACT-1", "Active 1",
                        new Vector3D(0, 0, 0), new Vector3D(0, 0, 0),
                        2.0, SpaceObject.Status.ACTIVE, null),
                new Satellite("SAT-ACT-2", "Active 2",
                        new Vector3D(20, 0, 0), new Vector3D(0, 0, 0),
                        2.0, SpaceObject.Status.ACTIVE, null),

                // Inactive objects very close to the active ones (5 km)
                new Debris("DEB-INACT-1",
                        new Vector3D(5, 0, 0), new Vector3D(0, 0, 0),
                        1.0, SpaceObject.Status.INACTIVE),
                new Satellite("SAT-INACT-2", "Inactive 2",
                        new Vector3D(22, 0, 0), new Vector3D(0, 0, 0),
                        2.0, SpaceObject.Status.INACTIVE, null),

                // Inactive objects close to each other
                new Debris("DEB-INACT-3",
                        new Vector3D(1000, 1000, 1000), new Vector3D(0, 0, 0),
                        1.0, SpaceObject.Status.INACTIVE),
                new Debris("DEB-INACT-4",
                        new Vector3D(1005, 1000, 1000), new Vector3D(0, 0, 0),
                        1.0, SpaceObject.Status.INACTIVE)
        );

        Set<SpatialGridIndex.CandidatePair> naivePairs = computeNaiveReferencePairs(objects);
        assertEquals(1, naivePairs.size());

        gridIndex.buildIndex(objects);
        List<SpatialGridIndex.CandidatePair> candidates = gridIndex.generateCandidatePairs();
        assertEquals(1, candidates.size());
        assertEquals("SAT-ACT-1", candidates.get(0).getObjectA().getId());
        assertEquals("SAT-ACT-2", candidates.get(0).getObjectB().getId());

        assertGridContainsAllNaiveQualifyingPairs(gridIndex, objects);
    }

    @Test
    void validateAgainstNaiveReferenceFastMovingOverflowObjects() {
        SpatialGridIndex gridIndex = new SpatialGridIndex();

        List<SpaceObject> objects = List.of(
                // Fast moving satellite exceeding max cells
                new Satellite("SAT-OVERFLOW-1", "Fast Sat 1",
                        new Vector3D(0, 0, 0), new Vector3D(80.0, 0, 0),
                        2.0, SpaceObject.Status.ACTIVE, null),

                // Slower satellite whose path intercepts the fast satellite's path within 30 km
                new Satellite("SAT-INTERCEPT", "Slow Intercept",
                        new Vector3D(8000, 25, 0), new Vector3D(0, 0, 0),
                        2.0, SpaceObject.Status.ACTIVE, null),

                // Second fast moving object
                new Debris("DEB-OVERFLOW-2",
                        new Vector3D(0, 20, 0), new Vector3D(80.0, 0, 0),
                        1.0, SpaceObject.Status.ACTIVE),

                // Distant satellite far from all trajectories
                new Satellite("SAT-DISTANT", "Distant Sat",
                        new Vector3D(0, 50000, 0), new Vector3D(0, 0, 0),
                        2.0, SpaceObject.Status.ACTIVE, null)
        );

        assertGridContainsAllNaiveQualifyingPairs(gridIndex, objects);
    }

    @Test
    void validateAgainstNaiveReferenceKnownThreatScenario() {
        SpatialGridIndex gridIndex = new SpatialGridIndex();

        ScenarioGenerator generator = new ScenarioGenerator();
        Scenario scenario = generator.generateKnownThreatScenario("SCENARIO-KT", 42L);

        List<SpaceObject> objects = scenario.getInitialObjectStates().stream()
                .map(InitialObjectState::toSpaceObject)
                .toList();

        assertGridContainsAllNaiveQualifyingPairs(gridIndex, objects);
    }

    @Test
    void validateAgainstNaiveReferenceRandomSeededScenarios() {
        SpatialGridIndex gridIndex = new SpatialGridIndex();
        ScenarioGenerator generator = new ScenarioGenerator();

        long[] testSeeds = {42L, 12345L, 98765L};

        for (long seed : testSeeds) {
            ScenarioGenerationConfig config = new ScenarioGenerationConfig(
                    20, 20,
                    -800.0, 800.0,
                    -2.0, 2.0,
                    0.5, 3.0
            );

            Scenario scenario = generator.generate("SCENARIO-TEST-" + seed, config, seed);

            List<SpaceObject> objects = scenario.getInitialObjectStates().stream()
                    .map(InitialObjectState::toSpaceObject)
                    .toList();

            assertGridContainsAllNaiveQualifyingPairs(gridIndex, objects);
        }
    }

}
