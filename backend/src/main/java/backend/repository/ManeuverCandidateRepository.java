package backend.repository;

import backend.model.ManeuverCandidate;

import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ManeuverCandidateRepository {

    private final String url =
            "jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require";

    private final String username = "postgres.fahdnplqhmxhixizdyxy";

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

    public void save(ManeuverCandidate candidate) throws Exception {

        String sql = """
                INSERT INTO maneuver_candidate
                (
                    maneuver_candidate_id,
                    encounter_id,
                    target_object_id,
                    delta_v_x,
                    delta_v_y,
                    delta_v_z,
                    new_min_separation_km,
                    cost,
                    rank
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection =
                     DriverManager.getConnection(url, username, password);
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, candidate.getManeuverCandidateId());
            statement.setString(2, candidate.getEncounterId());
            statement.setString(3, candidate.getTargetObjectId());
            statement.setDouble(4, candidate.getDeltaVX());
            statement.setDouble(5, candidate.getDeltaVY());
            statement.setDouble(6, candidate.getDeltaVZ());
            statement.setDouble(7, candidate.getNewMinSeparationKm());
            statement.setDouble(8, candidate.getCost());
            statement.setInt(9, candidate.getRank());

            statement.executeUpdate();
        }
    }
}
