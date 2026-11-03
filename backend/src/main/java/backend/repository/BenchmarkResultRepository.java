package backend.repository;

import backend.model.BenchmarkResult;

import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class BenchmarkResultRepository {

    private final String url =
            "jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require";

    private final String username = "postgres.fahdnplqhmxhixizdyxy";

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

    public void save(BenchmarkResult benchmark) throws Exception {

        String sql = """
                INSERT INTO benchmark_result
                (
                    benchmark_id,
                    run_id,
                    object_count,
                    naive_checks,
                    indexed_checks,
                    naive_ms,
                    indexed_ms
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection =
                     DriverManager.getConnection(url, username, password);
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, benchmark.getBenchmarkId());
            statement.setString(2, benchmark.getRunId());
            statement.setInt(3, benchmark.getObjectCount());
            statement.setLong(4, benchmark.getNaiveChecks());
            statement.setLong(5, benchmark.getIndexedChecks());
            statement.setDouble(6, benchmark.getNaiveMs());
            statement.setDouble(7, benchmark.getIndexedMs());

            statement.executeUpdate();
        }
    }
}
