package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpatialGridBenchmarkTest {

    @Test
    void executesBenchmarkDeterministicallyForIdenticalInputs() {
        SpatialGridBenchmark.BenchmarkResult run1 = SpatialGridBenchmark.runBenchmark(100, 42L);
        SpatialGridBenchmark.BenchmarkResult run2 = SpatialGridBenchmark.runBenchmark(100, 42L);

        assertEquals(100, run1.getObjectCount());
        assertEquals(42L, run1.getSeed());
        assertEquals(run1.getNaivePairs(), run2.getNaivePairs());
        assertEquals(run1.getGridCandidates(), run2.getGridCandidates());
        assertEquals(run1.getQualifyingEncounters(), run2.getQualifyingEncounters());
        assertEquals(run1.getPairReductionPercentage(), run2.getPairReductionPercentage(), 0.0001);
        assertTrue(run1.isCorrectnessVerified());
        assertTrue(run2.isCorrectnessVerified());
    }

    @Test
    void verifiesCorrectnessAndSignificantCandidateReduction() {
        int[] counts = {50, 100};
        List<SpatialGridBenchmark.BenchmarkResult> results = SpatialGridBenchmark.runAll(counts, 20261027L);

        assertEquals(2, results.size());

        for (SpatialGridBenchmark.BenchmarkResult r : results) {
            assertTrue(r.isCorrectnessVerified(),
                    "Grid candidates must include 100% of naive qualifying pairs.");

            long expectedNaivePairs = ((long) r.getObjectCount() * (r.getObjectCount() - 1)) / 2;
            assertEquals(expectedNaivePairs, r.getNaivePairs());

            assertTrue(r.getGridCandidates() <= r.getNaivePairs(),
                    "Grid candidate count should not exceed total naive pairs.");

            assertTrue(r.getPairReductionPercentage() > 80.0,
                    "Expected significant narrow-phase pair reduction (> 80%).");

            assertTrue(r.getQualifyingEncounters() <= r.getGridCandidates(),
                    "Qualifying encounters cannot exceed candidate pairs evaluated.");
        }
    }

    @Test
    void rejectsInvalidObjectCountsAndNullInputs() {
        assertThrows(IllegalArgumentException.class, () ->
                SpatialGridBenchmark.runBenchmark(1, 42L));
        assertThrows(IllegalArgumentException.class, () ->
                SpatialGridBenchmark.runBenchmark(0, 42L));
        assertThrows(IllegalArgumentException.class, () ->
                SpatialGridBenchmark.runBenchmark(-10, 42L));

        assertThrows(NullPointerException.class, () ->
                SpatialGridBenchmark.runAll(null, 42L));
    }

    @Test
    void enforcesBenchmarkResultValidationAndExposesFields() {
        SpatialGridBenchmark.BenchmarkResult result = new SpatialGridBenchmark.BenchmarkResult(
                200, 999L, 19900L, 1800L, 90.95, 12, 15L, 250L, true
        );

        assertEquals(200, result.getObjectCount());
        assertEquals(999L, result.getSeed());
        assertEquals(19900L, result.getNaivePairs());
        assertEquals(1800L, result.getGridCandidates());
        assertEquals(90.95, result.getPairReductionPercentage(), 0.001);
        assertEquals(12, result.getQualifyingEncounters());
        assertEquals(15L, result.getNaiveElapsedMs());
        assertEquals(250L, result.getGridElapsedMs());
        assertTrue(result.isCorrectnessVerified());

        // Negative metrics validation
        assertThrows(IllegalArgumentException.class, () ->
                new SpatialGridBenchmark.BenchmarkResult(1, 999L, 100, 10, 90.0, 1, 10, 20, true));
        assertThrows(IllegalArgumentException.class, () ->
                new SpatialGridBenchmark.BenchmarkResult(100, 999L, -1, 10, 90.0, 1, 10, 20, true));
        assertThrows(IllegalArgumentException.class, () ->
                new SpatialGridBenchmark.BenchmarkResult(100, 999L, 100, -5, 90.0, 1, 10, 20, true));
        assertThrows(IllegalArgumentException.class, () ->
                new SpatialGridBenchmark.BenchmarkResult(100, 999L, 100, 10, 90.0, -2, 10, 20, true));
    }
}

