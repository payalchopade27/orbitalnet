package backend.repository;

import backend.model.Encounter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

@Repository
public class EncounterRepository {

    private final String url =
            "jdbc:postgresql://db.fahdnplqhmxhixizdyxy.supabase.co:5432/postgres";

    private final String username = "postgres";

    private final String password =
            System.getenv("SUPABASE_DB_PASSWORD");

    public List<Encounter> findByRunId(String runId) throws Exception {

        List<Encounter> encounters = new ArrayList<>();

        String sql = """
                SELECT encounter_id,
                       run_id,
                       object_a_id,
                       object_b_id,
                       min_separation_km,
                       time_to_closest_approach_seconds,
                       relative_velocity_km_per_sec,
                       detected_at_tick,
                       status
                FROM encounter
                WHERE run_id = ?
                ORDER BY encounter_id
                """;

        try (
            Connection connection =
                    DriverManager.getConnection(url, username, password);

            PreparedStatement statement =
                    connection.prepareStatement(sql)
        ) {

            statement.setString(1, runId);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    Encounter encounter = new Encounter(
                            result.getString("encounter_id"),
                            result.getString("run_id"),
                            result.getString("object_a_id"),
                            result.getString("object_b_id"),
                            result.getDouble("min_separation_km"),
                            result.getDouble("time_to_closest_approach_seconds"),
                            result.getDouble("relative_velocity_km_per_sec"),
                            result.getLong("detected_at_tick"),
                            result.getString("status")
                    );

                    encounters.add(encounter);
                }
            }
        }

        return encounters;
    }

    public void save(Encounter encounter) throws Exception {

        String sql = """
                INSERT INTO encounter
                (
                    encounter_id,
                    run_id,
                    object_a_id,
                    object_b_id,
                    min_separation_km,
                    time_to_closest_approach_seconds,
                    relative_velocity_km_per_sec,
                    detected_at_tick,
                    status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (
            Connection connection =
                    DriverManager.getConnection(
                            url, username, password);

            PreparedStatement statement =
                    connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    encounter.getEncounterId()
            );

            statement.setString(
                    2,
                    encounter.getRunId()
            );

            statement.setString(
                    3,
                    encounter.getObjectAId()
            );

            statement.setString(
                    4,
                    encounter.getObjectBId()
            );

            statement.setDouble(
                    5,
                    encounter.getMinSeparationKm()
            );

            statement.setDouble(
                    6,
                    encounter.getTimeToClosestApproachSeconds()
            );

            statement.setDouble(
                    7,
                    encounter.getRelativeVelocityKmPerSec()
            );

            statement.setLong(
                    8,
                    encounter.getDetectedAtTick()
            );

            statement.setString(
                    9,
                    encounter.getStatus()
            );

            statement.executeUpdate();
        }
    }
}