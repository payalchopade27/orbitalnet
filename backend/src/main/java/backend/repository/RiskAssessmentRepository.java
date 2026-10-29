package backend.repository;

import backend.model.RiskAssessment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.springframework.stereotype.Repository;

@Repository
public class RiskAssessmentRepository {

    private final String url =
            "jdbc:postgresql://db.fahdnplqhmxhixizdyxy.supabase.co:5432/postgres";

    private final String username = "postgres";

    private final String password =
            System.getenv("SUPABASE_DB_PASSWORD");

    public RiskAssessment findByEncounterId(
            String encounterId) throws Exception {

        String sql = """
                SELECT encounter_id,
                       total_score,
                       separation_factor,
                       velocity_factor,
                       time_factor,
                       explanation
                FROM risk_assessment
                WHERE encounter_id = ?
                """;

        try (
            Connection connection =
                    DriverManager.getConnection(
                            url, username, password);

            PreparedStatement statement =
                    connection.prepareStatement(sql)
        ) {

            statement.setString(1, encounterId);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {

                    return new RiskAssessment(
                            result.getString("encounter_id"),
                            result.getInt("total_score"),
                            result.getDouble("separation_factor"),
                            result.getDouble("velocity_factor"),
                            result.getDouble("time_factor"),
                            result.getString("explanation")
                    );
                }
            }
        }

        return null;
    }

    public void save(RiskAssessment riskAssessment)
            throws Exception {

        String sql = """
                INSERT INTO risk_assessment
                (
                    encounter_id,
                    total_score,
                    separation_factor,
                    velocity_factor,
                    time_factor,
                    explanation
                )
                VALUES (?, ?, ?, ?, ?, ?)
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
                    riskAssessment.getEncounterId()
            );

            statement.setInt(
                    2,
                    riskAssessment.getTotalScore()
            );

            statement.setDouble(
                    3,
                    riskAssessment.getSeparationFactor()
            );

            statement.setDouble(
                    4,
                    riskAssessment.getVelocityFactor()
            );

            statement.setDouble(
                    5,
                    riskAssessment.getTimeFactor()
            );

            statement.setString(
                    6,
                    riskAssessment.getExplanation()
            );

            statement.executeUpdate();
        }
    }
}