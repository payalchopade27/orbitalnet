package backend.repository;

import backend.model.BenchmarkResult;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

public class BenchmarkResultRepository {

    private final String url =
            "jdbc:postgresql://db.fahdnplqhmxhixizdyxy.supabase.co:5432/postgres";

    private final String username = "postgres";

    private final String password =
            System.getenv("SUPABASE_DB_PASSWORD");

    public List<BenchmarkResult> findAll() throws Exception {

        List<BenchmarkResult> results = new ArrayList<>();

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

        try (Connection connection =
                     DriverManager.getConnection(url, username, password);
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {

                BenchmarkResult benchmark =
                        new BenchmarkResult(
                                result.getString("benchmark_id"),
                                result.getString("run_id"),
                                result.getInt("object_count"),
                                result.getLong("naive_checks"),
                                result.getLong("indexed_checks"),
                                result.getDouble("naive_ms"),
                                result.getDouble("indexed_ms")
                        );

                results.add(benchmark);
            }
        }

        return results;
    }
}