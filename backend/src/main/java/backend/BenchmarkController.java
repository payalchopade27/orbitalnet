package backend;

import backend.model.BenchmarkResult;
import backend.repository.BenchmarkResultRepository;
import com.orbitalnet.core.SpatialGridBenchmark;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/benchmarks")
public class BenchmarkController {

    private final BenchmarkResultRepository benchmarkResultRepository;
    private final List<Map<String, Object>> benchmarkCache = new CopyOnWriteArrayList<>();

    public BenchmarkController() {
        this.benchmarkResultRepository = new BenchmarkResultRepository();
    }

    public BenchmarkController(BenchmarkResultRepository benchmarkResultRepository) {
        this.benchmarkResultRepository = benchmarkResultRepository;
    }

    @GetMapping
    public ResponseEntity<?> getBenchmarks(
            @RequestParam(name = "force", defaultValue = "false") boolean force) {

        try {
            if (!force) {
                try {
                    List<BenchmarkResult> dbResults = benchmarkResultRepository.findAll();
                    if (!dbResults.isEmpty()) {
                        List<Map<String, Object>> response = new ArrayList<>();
                        for (BenchmarkResult r : dbResults) {
                            response.add(toMap(r));
                        }
                        return ResponseEntity.ok(response);
                    }
                } catch (Exception ignored) {
                    // Fall through to in-memory cache or real benchmark calculation
                }

                if (!benchmarkCache.isEmpty()) {
                    return ResponseEntity.ok(benchmarkCache);
                }
            }

            List<Map<String, Object>> generated =
                    executeRealBenchmark(new int[]{50, 100, 250});

            return ResponseEntity.ok(generated);

        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body(e.getMessage());
        }
    }

    @PostMapping({"", "/run"})
    public ResponseEntity<?> runBenchmarks(
            @RequestParam(name = "counts", required = false) int[] counts) {

        try {
            int[] targetCounts = (counts != null && counts.length > 0)
                    ? counts
                    : new int[]{50, 100, 250};

            List<Map<String, Object>> generated =
                    executeRealBenchmark(targetCounts);

            return ResponseEntity.ok(generated);

        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body(e.getMessage());
        }
    }

    private List<Map<String, Object>> executeRealBenchmark(int[] targetCounts) {

        List<SpatialGridBenchmark.BenchmarkResult> coreResults =
                SpatialGridBenchmark.runAll(targetCounts, SpatialGridBenchmark.DEFAULT_SEED);

        List<Map<String, Object>> responseList = new ArrayList<>();

        for (SpatialGridBenchmark.BenchmarkResult core : coreResults) {
            String benchmarkId = UUID.randomUUID().toString();

            BenchmarkResult model = new BenchmarkResult(
                    benchmarkId,
                    null,
                    core.getObjectCount(),
                    core.getNaivePairs(),
                    core.getGridCandidates(),
                    (double) core.getNaiveElapsedMs(),
                    (double) core.getGridElapsedMs()
            );

            try {
                benchmarkResultRepository.save(model);
            } catch (Exception ignored) {
                // DB offline or unreachable in local sandbox
            }

            responseList.add(toMap(model));
        }

        benchmarkCache.clear();
        benchmarkCache.addAll(responseList);

        return responseList;
    }

    private Map<String, Object> toMap(BenchmarkResult r) {
        Map<String, Object> map = new HashMap<>();
        map.put("benchmark_id", r.getBenchmarkId());
        map.put("run_id", r.getRunId());
        map.put("object_count", r.getObjectCount());
        map.put("naive_checks", r.getNaiveChecks());
        map.put("indexed_checks", r.getIndexedChecks());
        map.put("naive_ms", r.getNaiveMs());
        map.put("indexed_ms", r.getIndexedMs());
        return map;
    }
}
