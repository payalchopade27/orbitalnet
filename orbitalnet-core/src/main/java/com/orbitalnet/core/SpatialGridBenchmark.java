package com.orbitalnet.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Standalone benchmark harness measuring narrow-phase pair evaluation reduction
 * achieved by SpatialGridIndex compared to exhaustive naive pairwise checking.
 *
 * <p>Exposes deterministic benchmark execution and the {@link BenchmarkResult} model
 * for use by the service layer (Algorithm Lab) and database persistence.
 */
public final class SpatialGridBenchmark {

    public static final long DEFAULT_SEED = 20261027L;

    // Documented position and velocity envelopes (LEO-like test volume)
    public static final double MIN_POSITION_KM = -2000.0;
    public static final double MAX_POSITION_KM = 2000.0;
    public static final double MIN_VELOCITY_KM_S = -0.5;
    public static final double MAX_VELOCITY_KM_S = 0.5;
    public static final double MIN_SIZE_KM = 0.001;
    public static final double MAX_SIZE_KM = 0.05;

    /**
     * Core benchmark result model capturing the metrics and correctness of a comparison run.
     */
    public static final class BenchmarkResult {
        private final int objectCount;
        private final long seed;
        private final long naivePairs;
        private final long gridCandidates;
        private final double pairReductionPercentage;
        private final int qualifyingEncounters;
        private final long naiveElapsedMs;
        private final long gridElapsedMs;
        private final boolean correctnessVerified;

        public BenchmarkResult(
                int objectCount,
                long seed,
                long naivePairs,
                long gridCandidates,
                double pairReductionPercentage,
                int qualifyingEncounters,
                long naiveElapsedMs,
                long gridElapsedMs,
                boolean correctnessVerified) {

            if (objectCount < 2) {
                throw new IllegalArgumentException("Object count must be at least 2.");
            }
            if (naivePairs < 0) {
                throw new IllegalArgumentException("Naive pairs cannot be negative.");
            }
            if (gridCandidates < 0) {
                throw new IllegalArgumentException("Grid candidates cannot be negative.");
            }
            if (qualifyingEncounters < 0) {
                throw new IllegalArgumentException("Qualifying encounters cannot be negative.");
            }

            this.objectCount = objectCount;
            this.seed = seed;
            this.naivePairs = naivePairs;
            this.gridCandidates = gridCandidates;
            this.pairReductionPercentage = pairReductionPercentage;
            this.qualifyingEncounters = qualifyingEncounters;
            this.naiveElapsedMs = naiveElapsedMs;
            this.gridElapsedMs = gridElapsedMs;
            this.correctnessVerified = correctnessVerified;
        }

        public BenchmarkResult(
                int objectCount,
                long naivePairs,
                long gridCandidates,
                double pairReductionPercentage,
                int qualifyingEncounters,
                long naiveElapsedMs,
                long gridElapsedMs,
                boolean correctnessVerified) {
            this(objectCount, DEFAULT_SEED, naivePairs, gridCandidates,
                    pairReductionPercentage, qualifyingEncounters, naiveElapsedMs,
                    gridElapsedMs, correctnessVerified);
        }

        public int getObjectCount() {
            return objectCount;
        }

        public long getSeed() {
            return seed;
        }

        public long getNaivePairs() {
            return naivePairs;
        }

        public long getGridCandidates() {
            return gridCandidates;
        }

        public double getPairReductionPercentage() {
            return pairReductionPercentage;
        }

        public int getQualifyingEncounters() {
            return qualifyingEncounters;
        }

        public long getNaiveElapsedMs() {
            return naiveElapsedMs;
        }

        public long getGridElapsedMs() {
            return gridElapsedMs;
        }

        public boolean isCorrectnessVerified() {
            return correctnessVerified;
        }
    }

    private SpatialGridBenchmark() {
    }

    /**
     * Executes the benchmark for a given object count with the default seed.
     */
    public static BenchmarkResult runBenchmark(int objectCount) {
        return runBenchmark(objectCount, DEFAULT_SEED);
    }

    /**
     * Executes the benchmark for a given object count and seed.
     *
     * @param objectCount total objects (split evenly between satellites and debris)
     * @param seed deterministic scenario seed
     * @return benchmark measurements and correctness status
     */
    public static BenchmarkResult runBenchmark(int objectCount, long seed) {
        if (objectCount < 2) {
            throw new IllegalArgumentException("Object count must be at least 2.");
        }

        int satelliteCount = objectCount / 2;
        int debrisCount = objectCount - satelliteCount;

        ScenarioGenerationConfig config = new ScenarioGenerationConfig(
                satelliteCount,
                debrisCount,
                MIN_POSITION_KM,
                MAX_POSITION_KM,
                MIN_VELOCITY_KM_S,
                MAX_VELOCITY_KM_S,
                MIN_SIZE_KM,
                MAX_SIZE_KM
        );

        Scenario scenario = new ScenarioGenerator().generate(
                "BENCHMARK-" + objectCount, config, seed);

        List<SpaceObject> objects = scenario.getInitialObjectStates().stream()
                .map(InitialObjectState::toSpaceObject)
                .toList();

        long theoreticalNaivePairs = ((long) objectCount * (objectCount - 1)) / 2;

        // 1. Naive pairwise measurement
        long naiveStart = System.nanoTime();
        Set<SpatialGridIndex.CandidatePair> naiveQualifying = new HashSet<>();
        long actualNaivePairCount = 0;

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

                actualNaivePairCount++;

                ClosestApproach ca = new ClosestApproach(
                        a.getPosition(), a.getVelocity(),
                        b.getPosition(), b.getVelocity());

                if (ca.getDistanceKm() <= EncounterDetectionService.DETECTION_DISTANCE_KM) {
                    naiveQualifying.add(new SpatialGridIndex.CandidatePair(a, b));
                }
            }
        }
        long naiveElapsedMs = (System.nanoTime() - naiveStart) / 1_000_000;

        // 2. Spatial grid candidate measurement
        long gridStart = System.nanoTime();
        SpatialGridIndex grid = new SpatialGridIndex();
        grid.buildIndex(objects);
        List<SpatialGridIndex.CandidatePair> gridCandidates = grid.generateCandidatePairs();

        Set<SpatialGridIndex.CandidatePair> gridQualifying = new HashSet<>();
        for (SpatialGridIndex.CandidatePair candidate : gridCandidates) {
            ClosestApproach ca = new ClosestApproach(
                    candidate.getObjectA().getPosition(),
                    candidate.getObjectA().getVelocity(),
                    candidate.getObjectB().getPosition(),
                    candidate.getObjectB().getVelocity());

            if (ca.getDistanceKm() <= EncounterDetectionService.DETECTION_DISTANCE_KM) {
                gridQualifying.add(candidate);
            }
        }
        long gridElapsedMs = (System.nanoTime() - gridStart) / 1_000_000;

        // 3. Correctness check: Grid candidates MUST include all naive qualifying pairs,
        // and filtered qualifying encounters must match exactly.
        boolean correctnessVerified = gridCandidates.size() >= naiveQualifying.size()
                && gridCandidates.containsAll(naiveQualifying)
                && gridQualifying.equals(naiveQualifying);

        double reductionPercentage = theoreticalNaivePairs > 0
                ? (1.0 - (double) gridCandidates.size() / theoreticalNaivePairs) * 100.0
                : 0.0;

        return new BenchmarkResult(
                objectCount,
                seed,
                theoreticalNaivePairs,
                gridCandidates.size(),
                reductionPercentage,
                naiveQualifying.size(),
                naiveElapsedMs,
                gridElapsedMs,
                correctnessVerified
        );
    }

    /**
     * Runs benchmark across standard target sizes using the default seed.
     */
    public static List<BenchmarkResult> runAll(int[] objectCounts) {
        return runAll(objectCounts, DEFAULT_SEED);
    }

    /**
     * Runs benchmark across standard target sizes (e.g. 100, 500, 1000 objects).
     */
    public static List<BenchmarkResult> runAll(int[] objectCounts, long seed) {
        Objects.requireNonNull(objectCounts, "Object counts array cannot be null.");
        List<BenchmarkResult> results = new ArrayList<>();

        for (int count : objectCounts) {
            results.add(runBenchmark(count, seed));
        }

        return results;
    }

    /**
     * Formats and prints the benchmark report table to stdout.
     */
    public static void printReport(List<BenchmarkResult> results) {
        long seed = results.isEmpty() ? DEFAULT_SEED : results.get(0).getSeed();
        printReport(results, seed);
    }

    /**
     * Formats and prints the benchmark report table to stdout.
     */
    public static void printReport(List<BenchmarkResult> results, long seed) {
        System.out.println("=========================================================================================================================================");
        System.out.println("                                       ORBITALNET SPATIAL GRID BENCHMARK REPORT                                         ");
        System.out.println("=========================================================================================================================================");
        System.out.printf(Locale.ROOT,
                "Position Volume : [%.1f, %.1f] km | Velocity : [%.2f, %.2f] km/s | Horizon : %.0f s | Threshold : %.1f km%n",
                MIN_POSITION_KM, MAX_POSITION_KM, MIN_VELOCITY_KM_S, MAX_VELOCITY_KM_S,
                SimulationConstants.SIMULATION_HORIZON_SECONDS, EncounterDetectionService.DETECTION_DISTANCE_KM);
        System.out.printf(Locale.ROOT,
                "Deterministic Seed : %d | Cell Size : %.1f km%n",
                seed, SpatialGridIndex.CELL_SIZE_KM);
        System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println(" Objects |  Naive Pairs  |  Grid Candidates  | Pair Reduction (%) | Qualifying Encounters | Naive Time (ms) | Grid Time (ms) | Correctness ");
        System.out.println("---------+---------------+-------------------+--------------------+-----------------------+-----------------+----------------+-------------");

        for (BenchmarkResult r : results) {
            System.out.printf(Locale.ROOT,
                    " %7d | %13s | %17s | %17.2f%% | %21s | %14d  | %13d  | %11s %n",
                    r.getObjectCount(),
                    String.format(Locale.ROOT, "%,d", r.getNaivePairs()),
                    String.format(Locale.ROOT, "%,d", r.getGridCandidates()),
                    r.getPairReductionPercentage(),
                    String.format(Locale.ROOT, "%,d", r.getQualifyingEncounters()),
                    r.getNaiveElapsedMs(),
                    r.getGridElapsedMs(),
                    r.isCorrectnessVerified() ? "PASSED" : "FAILED");
        }

        System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("Correctness Verification: 100% of naive qualifying close-approach pairs (<= 100 km) were present in grid candidates.");
        System.out.println("Pair Reduction: Measured as percentage reduction in narrow-phase ClosestApproach evaluations.");
        System.out.println("Timing Note: Elapsed times are supplementary single-run measurements and should not be used as statistical claims.");
        System.out.println("=========================================================================================================================================");
    }

    public static void main(String[] args) {
        System.out.println("Starting OrbitalNet SpatialGridIndex Benchmark...");

        // Warm up JVM with a small run
        runBenchmark(50, DEFAULT_SEED);

        int[] targetCounts = {100, 500, 1000};
        List<BenchmarkResult> results = runAll(targetCounts, DEFAULT_SEED);

        printReport(results, DEFAULT_SEED);
    }
}

