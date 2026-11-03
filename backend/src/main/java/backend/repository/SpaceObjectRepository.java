package backend.repository;

import backend.model.SpaceObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

@Repository
public class SpaceObjectRepository {

    private final String url =
            "jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require";

    private final String username = "postgres.fahdnplqhmxhixizdyxy";

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

    public void save(SpaceObject object) throws Exception {
        if (object == null) {
            return;
        }
        saveAll(List.of(object));
    }

    public void saveAll(List<SpaceObject> objects) throws Exception {
        if (objects == null || objects.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO space_object
                (
                    object_id,
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
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (object_id) DO UPDATE SET
                    object_type = EXCLUDED.object_type,
                    name = EXCLUDED.name,
                    source_type = EXCLUDED.source_type,
                    size = EXCLUDED.size,
                    status = EXCLUDED.status,
                    pos_x = EXCLUDED.pos_x,
                    pos_y = EXCLUDED.pos_y,
                    pos_z = EXCLUDED.pos_z,
                    vel_x = EXCLUDED.vel_x,
                    vel_y = EXCLUDED.vel_y,
                    vel_z = EXCLUDED.vel_z,
                    altitude_km = EXCLUDED.altitude_km,
                    inclination_deg = EXCLUDED.inclination_deg,
                    period_seconds = EXCLUDED.period_seconds
                WHERE space_object.scenario_id = EXCLUDED.scenario_id
                """;

        try (
            Connection connection =
                    DriverManager.getConnection(url, username, password);
            PreparedStatement statement =
                    connection.prepareStatement(sql)
        ) {
            connection.setAutoCommit(false);

            for (SpaceObject object : objects) {
                statement.setString(1, object.getObjectId());
                statement.setString(2, object.getScenarioId());
                statement.setString(3, object.getObjectType());
                statement.setString(4, object.getName());
                statement.setString(5, object.getSourceType());
                statement.setDouble(6, object.getSize());
                statement.setString(7, object.getStatus());
                statement.setDouble(8, object.getPosX());
                statement.setDouble(9, object.getPosY());
                statement.setDouble(10, object.getPosZ());
                statement.setDouble(11, object.getVelX());
                statement.setDouble(12, object.getVelY());
                statement.setDouble(13, object.getVelZ());
                statement.setObject(14, object.getAltitudeKm());
                statement.setObject(15, object.getInclinationDeg());
                statement.setObject(16, object.getPeriodSeconds());

                statement.addBatch();
            }

            statement.executeBatch();
            connection.commit();
        }
    }

    public SpaceObject findById(String objectId) throws Exception {
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
                WHERE object_id = ?
                """;

        try (
            Connection connection =
                    DriverManager.getConnection(url, username, password);
            PreparedStatement statement =
                    connection.prepareStatement(sql)
        ) {
            statement.setString(1, objectId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return new SpaceObject(
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
                }
            }
        }

        return null;
    }
}
