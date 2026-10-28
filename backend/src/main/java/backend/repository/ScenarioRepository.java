package backend.repository;

import backend.model.Scenario;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

@Repository
public class ScenarioRepository {

    private final String url =
            "jdbc:postgresql://db.fahdnplqhmxhixizdyxy.supabase.co:5432/postgres";

    private final String username = "postgres";

    private final String password =
            System.getenv("SUPABASE_DB_PASSWORD");

    public List<Scenario> findAll() throws Exception {

        List<Scenario> scenarios = new ArrayList<>();

        String sql = """
                SELECT scenario_id, name, seed,
                       generation_config, status, created_at
                FROM scenario
                ORDER BY scenario_id
                """;

        try (
            Connection connection =
                    DriverManager.getConnection(url, username, password);

            PreparedStatement statement =
                    connection.prepareStatement(sql);

            ResultSet result =
                    statement.executeQuery()
        ) {

            while (result.next()) {

                Scenario scenario = new Scenario(
                        result.getString("scenario_id"),
                        result.getString("name"),
                        result.getLong("seed"),
                        result.getString("generation_config"),
                        result.getString("status"),
                        result.getString("created_at")
                );

                scenarios.add(scenario);
            }
        }

        return scenarios;
    }

    public void save(Scenario scenario) throws Exception {

        String sql = """
                INSERT INTO scenario
                (scenario_id, name, seed, generation_config, status)
                VALUES (?, ?, ?, ?::jsonb, ?)
                """;

        try (
            Connection connection =
                    DriverManager.getConnection(url, username, password);

            PreparedStatement statement =
                    connection.prepareStatement(sql)
        ) {

            statement.setString(1, scenario.getScenarioId());
            statement.setString(2, scenario.getName());
            statement.setLong(3, scenario.getSeed());
            statement.setString(4, scenario.getGenerationConfig());
            statement.setString(5, scenario.getStatus());

            statement.executeUpdate();
        }
    }

    public Scenario findById(String scenarioId) throws Exception {

        String sql = """
                SELECT scenario_id,
                       name,
                       seed,
                       generation_config,
                       status,
                       created_at
                FROM scenario
                WHERE scenario_id = ?
                """;

        try (
            Connection connection =
                    DriverManager.getConnection(url, username, password);

            PreparedStatement statement =
                    connection.prepareStatement(sql)
        ) {

            statement.setString(1, scenarioId);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {

                    return new Scenario(
                            result.getString("scenario_id"),
                            result.getString("name"),
                            result.getLong("seed"),
                            result.getString("generation_config"),
                            result.getString("status"),
                            result.getString("created_at")
                    );
                }
            }
        }

        return null;
    }
}