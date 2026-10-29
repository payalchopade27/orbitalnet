package backend.repository;

import backend.model.ManeuverCandidate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

public class ManeuverCandidateRepository {

    private final String url =
            "jdbc:postgresql://db.fahdnplqhmxhixizdyxy.supabase.co:5432/postgres";

    private final String username = "postgres";

    private final String password =
            System.getenv("SUPABASE_DB_PASSWORD");

    public List<ManeuverCandidate> findByEncounterId(String encounterId)
            throws Exception {

        List<ManeuverCandidate> candidates = new ArrayList<>();

        String sql = """
                SELECT maneuver_candidate_id,
                       encounter_id,
                       target_object_id,
                       delta_v_x,
                       delta_v_y,
                       delta_v_z,
                       new_min_separation_km,
                       cost,
                       rank
                FROM maneuver_candidate
                WHERE encounter_id = ?
                ORDER BY rank
                """;

        try (Connection connection =
                     DriverManager.getConnection(url, username, password);
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, encounterId);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    ManeuverCandidate candidate =
                            new ManeuverCandidate(
                                    result.getString("maneuver_candidate_id"),
                                    result.getString("encounter_id"),
                                    result.getString("target_object_id"),
                                    result.getDouble("delta_v_x"),
                                    result.getDouble("delta_v_y"),
                                    result.getDouble("delta_v_z"),
                                    result.getDouble("new_min_separation_km"),
                                    result.getDouble("cost"),
                                    result.getInt("rank")
                            );

                    candidates.add(candidate);
                }
            }
        }

        return candidates;
    }
}