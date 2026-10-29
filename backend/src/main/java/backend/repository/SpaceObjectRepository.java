package backend.repository;

import backend.model.SpaceObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SpaceObjectRepository {

    private final String url =
            "jdbc:postgresql://db.fahdnplqhmxhixizdyxy.supabase.co:5432/postgres";

    private final String username = "postgres";

    private final String password =
            System.getenv("SUPABASE_DB_PASSWORD");

    public List<SpaceObject> findByScenarioId(String scenarioId) throws Exception {

        List<SpaceObject> objects = new ArrayList<>();

        String sql = """
                SELECT object_id,
                       scenario_id,
                       object_type,
                       name,
                       source_type,
                       size,
                       status,
                       pos_x,
                       pos_y,
                       pos_z,
                       vel_x,
                       vel_y,
                       vel_z,
                       altitude_km,
                       inclination_deg,
                       period_seconds
                FROM space_object
                WHERE scenario_id = ?
                ORDER BY object_id
                """;

        try (
            Connection connection =
                    DriverManager.getConnection(url, username, password);

            PreparedStatement statement =
                    connection.prepareStatement(sql)
        ) {

            statement.setString(1, scenarioId);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    SpaceObject object = new SpaceObject(
                            result.getString("object_id"),
                            result.getString("scenario_id"),
                            result.getString("object_type"),
                            result.getString("name"),
                            result.getString("source_type"),
                            result.getDouble("size"),
                            result.getString("status"),

                            result.getDouble("pos_x"),
                            result.getDouble("pos_y"),
                            result.getDouble("pos_z"),

                            result.getDouble("vel_x"),
                            result.getDouble("vel_y"),
                            result.getDouble("vel_z"),

                            (Double) result.getObject("altitude_km"),
                            (Double) result.getObject("inclination_deg"),
                            (Double) result.getObject("period_seconds")
                    );

                    objects.add(object);
                }
            }
        }

        return objects;
    }
}