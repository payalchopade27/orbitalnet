package backend;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/api/benchmarks")
public class BenchmarkController {

    private final String url =
            "jdbc:postgresql://db.fahdnplqhmxhixizdyxy.supabase.co:5432/postgres";

    private final String username = "postgres";

    private final String password =
            System.getenv("SUPABASE_DB_PASSWORD");

    @GetMapping
    public ResponseEntity<?> getBenchmarks() {

        try {

            List<Map<String, Object>> benchmarks =
                    new ArrayList<>();

            String sql = """
                    SELECT benchmark_id,
                           run_id,
                           object_count,
                           naive_checks,
                           indexed_checks,
                           naive_ms,
                           indexed_ms
                    FROM benchmark_result
                    ORDER BY benchmark_id
                    """;

            try (
                    Connection connection =
                            DriverManager.getConnection(
                                    url,
                                    username,
                                    password);

                    PreparedStatement statement =
                            connection.prepareStatement(sql);

                    ResultSet result =
                            statement.executeQuery()
            ) {

                while (result.next()) {

                    Map<String, Object> benchmark =
                            new HashMap<>();

                    benchmark.put(
                            "benchmark_id",
                            result.getString(
                                    "benchmark_id"));

                    benchmark.put(
                            "run_id",
                            result.getString("run_id"));

                    benchmark.put(
                            "object_count",
                            result.getInt(
                                    "object_count"));

                    benchmark.put(
                            "naive_checks",
                            result.getLong(
                                    "naive_checks"));

                    benchmark.put(
                            "indexed_checks",
                            result.getLong(
                                    "indexed_checks"));

                    benchmark.put(
                            "naive_ms",
                            result.getDouble(
                                    "naive_ms"));

                    benchmark.put(
                            "indexed_ms",
                            result.getDouble(
                                    "indexed_ms"));

                    benchmarks.add(benchmark);
                }
            }

            return ResponseEntity.ok(benchmarks);

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .body(e.getMessage());
        }
    }
}