package backend;

import backend.model.Encounter;
import backend.model.RiskAssessment;
import backend.repository.EncounterRepository;
import backend.repository.RiskAssessmentRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/api/encounters")
public class EncounterController {

    private final EncounterRepository encounterRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;

    private final String url =
            "jdbc:postgresql://db.fahdnplqhmxhixizdyxy.supabase.co:5432/postgres";

    private final String username = "postgres";

    private final String password =
            System.getenv("SUPABASE_DB_PASSWORD");

    public EncounterController(
            EncounterRepository encounterRepository,
            RiskAssessmentRepository riskAssessmentRepository) {

        this.encounterRepository = encounterRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
    }

    // =========================================================
    // GET ENCOUNTER
    // =========================================================

    @GetMapping("/{encounterId}")
    public ResponseEntity<?> getEncounter(
            @PathVariable String encounterId) {

        try {

            Encounter encounter = null;

            for (Encounter e :
                    encounterRepository.findByRunId(
                            getRunIdFromEncounter(encounterId))) {

                if (e.getEncounterId().equals(encounterId)) {
                    encounter = e;
                    break;
                }
            }

            if (encounter == null) {
                return ResponseEntity.notFound().build();
            }

            RiskAssessment risk =
                    riskAssessmentRepository
                            .findByEncounterId(encounterId);

            Map<String, Object> response =
                    new HashMap<>();

            response.put("encounterId",
                    encounter.getEncounterId());

            response.put("runId",
                    encounter.getRunId());

            response.put("objectAId",
                    encounter.getObjectAId());

            response.put("objectBId",
                    encounter.getObjectBId());

            response.put("detectedAtTick",
                    encounter.getDetectedAtTick());

            response.put("minSeparationKm",
                    encounter.getMinSeparationKm());

            response.put(
                    "timeToClosestApproachSeconds",
                    encounter.getTimeToClosestApproachSeconds());

            response.put(
                    "relativeVelocityKmPerSec",
                    encounter.getRelativeVelocityKmPerSec());

            response.put("status",
                    encounter.getStatus());

            if (risk != null) {

                Map<String, Object> riskResponse =
                        new HashMap<>();

                riskResponse.put(
                        "totalScore",
                        risk.getTotalScore());

                riskResponse.put(
                        "separationFactor",
                        risk.getSeparationFactor());

                riskResponse.put(
                        "velocityFactor",
                        risk.getVelocityFactor());

                riskResponse.put(
                        "timeFactor",
                        risk.getTimeFactor());

                riskResponse.put(
                        "explanation",
                        risk.getExplanation());

                response.put("risk", riskResponse);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .body(e.getMessage());
        }
    }

    // =========================================================
    // CREATE / GET MANEUVER CANDIDATES
    // =========================================================

    @PostMapping("/{encounterId}/maneuvers")
    public ResponseEntity<?> createManeuvers(
            @PathVariable String encounterId) {

        try {

            List<Map<String, Object>> candidates =
                    new ArrayList<>();

            String checkSql = """
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

            try (
                    Connection connection =
                            DriverManager.getConnection(
                                    url, username, password);

                    PreparedStatement statement =
                            connection.prepareStatement(checkSql)
            ) {

                statement.setString(1, encounterId);

                try (ResultSet result =
                             statement.executeQuery()) {

                    while (result.next()) {

                        candidates.add(
                                candidateMap(result)
                        );
                    }
                }
            }

            // Already generated → return them
            if (!candidates.isEmpty()) {
                return ResponseEntity.ok(candidates);
            }

            // Get encounter objects
            String objectSql = """
                    SELECT object_a_id, object_b_id
                    FROM encounter
                    WHERE encounter_id = ?
                    """;

            String targetObject;

            try (
                    Connection connection =
                            DriverManager.getConnection(
                                    url, username, password);

                    PreparedStatement statement =
                            connection.prepareStatement(objectSql)
            ) {

                statement.setString(1, encounterId);

                try (ResultSet result =
                             statement.executeQuery()) {

                    if (!result.next()) {
                        return ResponseEntity.notFound().build();
                    }

                    targetObject =
                            result.getString("object_b_id");
                }
            }

            // Candidate 1
            insertCandidate(
                    encounterId,
                    targetObject,
                    0.0,
                    5.0,
                    0.0,
                    100.0,
                    5.0,
                    1
            );

            // Candidate 2
            insertCandidate(
                    encounterId,
                    targetObject,
                    0.0,
                    2.5,
                    0.0,
                    50.0,
                    2.5,
                    2
            );

            // Read them back
            try (
                    Connection connection =
                            DriverManager.getConnection(
                                    url, username, password);

                    PreparedStatement statement =
                            connection.prepareStatement(checkSql)
            ) {

                statement.setString(1, encounterId);

                try (ResultSet result =
                             statement.executeQuery()) {

                    while (result.next()) {

                        candidates.add(
                                candidateMap(result)
                        );
                    }
                }
            }

            return ResponseEntity.ok(candidates);

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .body(e.getMessage());
        }
    }

    // =========================================================
    // APPLY MANEUVER
    // =========================================================

    @PostMapping(
            "/{encounterId}/maneuvers/{maneuverCandidateId}/apply")
    public ResponseEntity<?> applyManeuver(
            @PathVariable String encounterId,
            @PathVariable String maneuverCandidateId) {

        try {

            String baselineRunId;
            String scenarioId;
            long currentTick;
            double elapsedTime;

            // ---------------------------------------------
            // Find encounter → baseline run
            // ---------------------------------------------

            String encounterSql = """
                    SELECT run_id
                    FROM encounter
                    WHERE encounter_id = ?
                    """;

            try (
                    Connection connection =
                            DriverManager.getConnection(
                                    url, username, password);

                    PreparedStatement statement =
                            connection.prepareStatement(
                                    encounterSql)
            ) {

                statement.setString(1, encounterId);

                try (ResultSet result =
                             statement.executeQuery()) {

                    if (!result.next()) {
                        return ResponseEntity
                                .notFound()
                                .build();
                    }

                    baselineRunId =
                            result.getString("run_id");
                }
            }

            // ---------------------------------------------
            // Get baseline run
            // ---------------------------------------------

            String runSql = """
                    SELECT scenario_id,
                           current_tick,
                           elapsed_time_seconds
                    FROM simulation_run
                    WHERE run_id = ?
                    """;

            try (
                    Connection connection =
                            DriverManager.getConnection(
                                    url, username, password);

                    PreparedStatement statement =
                            connection.prepareStatement(runSql)
            ) {

                statement.setString(1, baselineRunId);

                try (ResultSet result =
                             statement.executeQuery()) {

                    if (!result.next()) {
                        return ResponseEntity
                                .notFound()
                                .build();
                    }

                    scenarioId =
                            result.getString("scenario_id");

                    currentTick =
                            result.getLong("current_tick");

                    elapsedTime =
                            result.getDouble(
                                    "elapsed_time_seconds");
                }
            }

            // ---------------------------------------------
            // Get candidate
            // ---------------------------------------------

            String targetObject;
            double dvX;
            double dvY;
            double dvZ;

            String candidateSql = """
                    SELECT target_object_id,
                           delta_v_x,
                           delta_v_y,
                           delta_v_z
                    FROM maneuver_candidate
                    WHERE maneuver_candidate_id = ?
                      AND encounter_id = ?
                    """;

            try (
                    Connection connection =
                            DriverManager.getConnection(
                                    url, username, password);

                    PreparedStatement statement =
                            connection.prepareStatement(
                                    candidateSql)
            ) {

                statement.setString(1,
                        maneuverCandidateId);

                statement.setString(2,
                        encounterId);

                try (ResultSet result =
                             statement.executeQuery()) {

                    if (!result.next()) {
                        return ResponseEntity
                                .notFound()
                                .build();
                    }

                    targetObject =
                            result.getString(
                                    "target_object_id");

                    dvX =
                            result.getDouble("delta_v_x");

                    dvY =
                            result.getDouble("delta_v_y");

                    dvZ =
                            result.getDouble("delta_v_z");
                }
            }

            // ---------------------------------------------
            // Create new maneuver run
            // ---------------------------------------------

            String newRunId =
                    UUID.randomUUID().toString();

            String insertRunSql = """
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
                            connection.prepareStatement(
                                    insertRunSql)
            ) {

                statement.setString(1, newRunId);
                statement.setString(2, scenarioId);
                statement.setString(3, "COMPLETED");
                statement.setLong(4, currentTick);
                statement.setDouble(5, elapsedTime);
                statement.setString(6, "MANEUVER");
                statement.setString(7, baselineRunId);
                statement.setString(
                        8,
                        maneuverCandidateId);

                statement.executeUpdate();
            }

            // ---------------------------------------------
            // Create final states
            // ---------------------------------------------

            String objectSql = """
                    SELECT object_id,
                           status,
                           pos_x,
                           pos_y,
                           pos_z,
                           vel_x,
                           vel_y,
                           vel_z
                    FROM space_object
                    WHERE scenario_id = ?
                    """;

            String insertStateSql = """
                    INSERT INTO final_object_state
                    (
                        run_id,
                        object_id,
                        pos_x,
                        pos_y,
                        pos_z,
                        vel_x,
                        vel_y,
                        vel_z,
                        status
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            try (
                    Connection connection =
                            DriverManager.getConnection(
                                    url, username, password);

                    PreparedStatement objectStatement =
                            connection.prepareStatement(
                                    objectSql);

                    PreparedStatement stateStatement =
                            connection.prepareStatement(
                                    insertStateSql)
            ) {

                objectStatement.setString(
                        1, scenarioId);

                try (ResultSet result =
                             objectStatement.executeQuery()) {

                    while (result.next()) {

                        String objectId =
                                result.getString("object_id");

                        double x =
                                result.getDouble("pos_x");

                        double y =
                                result.getDouble("pos_y");

                        double z =
                                result.getDouble("pos_z");

                        double vx =
                                result.getDouble("vel_x");

                        double vy =
                                result.getDouble("vel_y");

                        double vz =
                                result.getDouble("vel_z");

                        // Move object using elapsed time
                        x += vx * elapsedTime;
                        y += vy * elapsedTime;
                        z += vz * elapsedTime;

                        // Apply maneuver
                        if (objectId.equals(
                                targetObject)) {

                            vx += dvX;
                            vy += dvY;
                            vz += dvZ;
                        }

                        stateStatement.setString(
                                1, newRunId);

                        stateStatement.setString(
                                2, objectId);

                        stateStatement.setDouble(
                                3, x);

                        stateStatement.setDouble(
                                4, y);

                        stateStatement.setDouble(
                                5, z);

                        stateStatement.setDouble(
                                6, vx);

                        stateStatement.setDouble(
                                7, vy);

                        stateStatement.setDouble(
                                8, vz);

                        stateStatement.setString(
                                9,
                                result.getString(
                                        "status"));

                        stateStatement.addBatch();
                    }

                    stateStatement.executeBatch();
                }
            }

            Map<String, Object> response =
                    new HashMap<>();

            response.put(
                    "message",
                    "Maneuver applied successfully");

            response.put(
                    "baselineRunId",
                    baselineRunId);

            response.put(
                    "maneuverRunId",
                    newRunId);

            response.put(
                    "maneuverCandidateId",
                    maneuverCandidateId);

            response.put(
                    "targetObjectId",
                    targetObject);

            response.put(
                    "deltaV",
                    Map.of(
                            "x", dvX,
                            "y", dvY,
                            "z", dvZ
                    )
            );

            response.put(
                    "status",
                    "COMPLETED");

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .body(e.getMessage());
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private Map<String, Object> candidateMap(
            ResultSet result) throws Exception {

        Map<String, Object> candidate =
                new HashMap<>();

        candidate.put(
                "maneuverCandidateId",
                result.getString(
                        "maneuver_candidate_id"));

        candidate.put(
                "encounterId",
                result.getString("encounter_id"));

        candidate.put(
                "targetObjectId",
                result.getString(
                        "target_object_id"));

        candidate.put(
                "deltaV",
                Map.of(
                        "x",
                        result.getDouble("delta_v_x"),
                        "y",
                        result.getDouble("delta_v_y"),
                        "z",
                        result.getDouble("delta_v_z")
                )
        );

        candidate.put(
                "newMinSeparationKm",
                result.getDouble(
                        "new_min_separation_km"));

        candidate.put(
                "cost",
                result.getDouble("cost"));

        candidate.put(
                "rank",
                result.getInt("rank"));

        return candidate;
    }

    private void insertCandidate(
            String encounterId,
            String targetObjectId,
            double dvX,
            double dvY,
            double dvZ,
            double newMinSeparation,
            double cost,
            int rank) throws Exception {

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

        try (
                Connection connection =
                        DriverManager.getConnection(
                                url, username, password);

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    UUID.randomUUID().toString());

            statement.setString(2, encounterId);
            statement.setString(3, targetObjectId);
            statement.setDouble(4, dvX);
            statement.setDouble(5, dvY);
            statement.setDouble(6, dvZ);
            statement.setDouble(7, newMinSeparation);
            statement.setDouble(8, cost);
            statement.setInt(9, rank);

            statement.executeUpdate();
        }
    }

    private String getRunIdFromEncounter(
            String encounterId) throws Exception {

        String[] parts = encounterId.split("-");

        if (parts.length < 5) {
            throw new IllegalArgumentException(
                    "Invalid encounter ID");
        }

        return findRunIdDirectly(encounterId);
    }

    private String findRunIdDirectly(
            String encounterId) throws Exception {

        String sql =
                "SELECT run_id FROM encounter " +
                "WHERE encounter_id = ?";

        try (
                Connection connection =
                        DriverManager.getConnection(
                                url,
                                username,
                                password);

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, encounterId);

            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                if (result.next()) {
                    return result.getString("run_id");
                }
            }
        }

        return "";
    }
}