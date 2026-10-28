package backend.repository;

import backend.model.FinalObjectState;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

public class FinalObjectStateRepository {

    private final String url =
            "jdbc:postgresql://db.fahdnplqhmxhixizdyxy.supabase.co:5432/postgres";

    private final String username = "postgres";

    private final String password =
            System.getenv("SUPABASE_DB_PASSWORD");

    public List<FinalObjectState> findByRunId(String runId)
            throws Exception {

        List<FinalObjectState> states = new ArrayList<>();

        String sql = """
                SELECT run_id,
                       object_id,
                       pos_x,
                       pos_y,
                       pos_z,
                       vel_x,
                       vel_y,
                       vel_z,
                       status
                FROM final_object_state
                WHERE run_id = ?
                ORDER BY object_id
                """;

        try (Connection connection =
                     DriverManager.getConnection(url, username, password);
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, runId);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    FinalObjectState state =
                            new FinalObjectState(
                                    result.getString("run_id"),
                                    result.getString("object_id"),
                                    result.getDouble("pos_x"),
                                    result.getDouble("pos_y"),
                                    result.getDouble("pos_z"),
                                    result.getDouble("vel_x"),
                                    result.getDouble("vel_y"),
                                    result.getDouble("vel_z"),
                                    result.getString("status")
                            );

                    states.add(state);
                }
            }
        }

        return states;
    }
}