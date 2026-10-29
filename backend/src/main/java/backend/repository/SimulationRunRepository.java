package backend.repository;

import backend.model.SimulationRun;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

@Repository
public class SimulationRunRepository {

    private final String url =
            "jdbc:postgresql://db.fahdnplqhmxhixizdyxy.supabase.co:5432/postgres";

    private final String username = "postgres";

    private final String password =
            System.getenv("SUPABASE_DB_PASSWORD");

    public List<SimulationRun> findByScenarioId(
            String scenarioId) throws Exception {

        List<SimulationRun> runs = new ArrayList<>();

        String sql = """
                SELECT run_id,
                       scenario_id,
                       status,
                       current_tick,
                       elapsed_time_seconds,
                       algorithm_used,
                       baseline_run_id,
                       maneuver_candidate_id
                FROM simulation_run
                WHERE scenario_id = ?
                ORDER BY run_id
                """;

        try (
            Connection connection =
                    DriverManager.getConnection(
                            url, username, password);

            PreparedStatement statement =
                    connection.prepareStatement(sql)
        ) {

            statement.setString(1, scenarioId);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    SimulationRun run = new SimulationRun(
                            result.getString("run_id"),
                            result.getString("scenario_id"),
                            result.getString("status"),
                            result.getLong("current_tick"),
                            result.getDouble("elapsed_time_seconds"),
                            result.getString("algorithm_used"),
                            result.getString("baseline_run_id"),
                            result.getString("maneuver_candidate_id")
                    );

                    runs.add(run);
                }
            }
        }

        return runs;
    }

    public void save(SimulationRun run) throws Exception {

        String sql = """
                INSERT INTO simulation_run
                (
                    run_id,
                    scenario_id,
                    status,
                    current_tick,
                    elapsed_time_seconds,
                    algorithm_used,
                    baseline_run_id,
                    maneuver_candidate_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (
            Connection connection =
                    DriverManager.getConnection(
                            url, username, password);

            PreparedStatement statement =
                    connection.prepareStatement(sql)
        ) {

            statement.setString(1, run.getRunId());
            statement.setString(2, run.getScenarioId());
            statement.setString(3, run.getStatus());
            statement.setLong(4, run.getCurrentTick());
            statement.setDouble(
                    5,
                    run.getElapsedTimeSeconds()
            );
            statement.setString(
                    6,
                    run.getAlgorithmUsed()
            );
            statement.setString(
                    7,
                    run.getBaselineRunId()
            );
            statement.setString(
                    8,
                    run.getManeuverCandidateId()
            );

            statement.executeUpdate();
        }
    }

    public SimulationRun findById(String runId)
            throws Exception {

        String sql = """
                SELECT run_id,
                       scenario_id,
                       status,
                       current_tick,
                       elapsed_time_seconds,
                       algorithm_used,
                       baseline_run_id,
                       maneuver_candidate_id
                FROM simulation_run
                WHERE run_id = ?
                """;

        try (
            Connection connection =
                    DriverManager.getConnection(
                            url, username, password);

            PreparedStatement statement =
                    connection.prepareStatement(sql)
        ) {

            statement.setString(1, runId);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {

                    return new SimulationRun(
                            result.getString("run_id"),
                            result.getString("scenario_id"),
                            result.getString("status"),
                            result.getLong("current_tick"),
                            result.getDouble("elapsed_time_seconds"),
                            result.getString("algorithm_used"),
                            result.getString("baseline_run_id"),
                            result.getString("maneuver_candidate_id")
                    );
                }
            }
        }

        return null;
    }

    public void updateProgress(
            String runId,
            String status,
            long currentTick,
            double elapsedTimeSeconds
    ) throws Exception {

        String sql = """
                UPDATE simulation_run
                SET status = ?,
                    current_tick = ?,
                    elapsed_time_seconds = ?
                WHERE run_id = ?
                """;

        try (
            Connection connection =
                    DriverManager.getConnection(
                            url, username, password);

            PreparedStatement statement =
                    connection.prepareStatement(sql)
        ) {

            statement.setString(1, status);
            statement.setLong(2, currentTick);
            statement.setDouble(
                    3,
                    elapsedTimeSeconds
            );
            statement.setString(4, runId);

            statement.executeUpdate();
        }
    }
}