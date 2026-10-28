package backend.model;

public class BenchmarkResult {

    private String benchmarkId;
    private String runId;
    private int objectCount;
    private long naiveChecks;
    private long indexedChecks;
    private double naiveMs;
    private double indexedMs;

    public BenchmarkResult() {
    }

    public BenchmarkResult(
            String benchmarkId,
            String runId,
            int objectCount,
            long naiveChecks,
            long indexedChecks,
            double naiveMs,
            double indexedMs) {

        this.benchmarkId = benchmarkId;
        this.runId = runId;
        this.objectCount = objectCount;
        this.naiveChecks = naiveChecks;
        this.indexedChecks = indexedChecks;
        this.naiveMs = naiveMs;
        this.indexedMs = indexedMs;
    }

    public String getBenchmarkId() {
        return benchmarkId;
    }

    public String getRunId() {
        return runId;
    }

    public int getObjectCount() {
        return objectCount;
    }

    public long getNaiveChecks() {
        return naiveChecks;
    }

    public long getIndexedChecks() {
        return indexedChecks;
    }

    public double getNaiveMs() {
        return naiveMs;
    }

    public double getIndexedMs() {
        return indexedMs;
    }
}