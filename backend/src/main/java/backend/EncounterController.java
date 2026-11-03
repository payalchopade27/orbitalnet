package backend;

import backend.model.Encounter;
import backend.model.ManeuverCandidate;
import backend.model.RiskAssessment;
import backend.repository.EncounterRepository;
import backend.repository.ManeuverCandidateRepository;
import backend.repository.RiskAssessmentRepository;
import com.orbitalnet.core.Debris;
import com.orbitalnet.core.ManeuverPlanner;
import com.orbitalnet.core.Satellite;
import com.orbitalnet.core.SpaceObject;
import com.orbitalnet.core.Vector3D;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/api/encounters")
public class EncounterController {

    private final EncounterRepository encounterRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final ManeuverCandidateRepository maneuverCandidateRepository;
    private final SimulationRunService simulationRunService;

    private final String url =
            "jdbc:postgresql://db.fahdnplqhmxhixizdyxy.supabase.co:5432/postgres";

    private final String username = "postgres";

    private final String password =
            System.getenv("SUPABASE_DB_PASSWORD");

    public EncounterController(
            EncounterRepository encounterRepository,
            RiskAssessmentRepository riskAssessmentRepository,
            ManeuverCandidateRepository maneuverCandidateRepository,
            SimulationRunService simulationRunService) {

        this.encounterRepository = encounterRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.maneuverCandidateRepository = maneuverCandidateRepository;
        this.simulationRunService = simulationRunService;
    }

    public EncounterController(
            EncounterRepository encounterRepository,
            RiskAssessmentRepository riskAssessmentRepository) {

        this(encounterRepository, riskAssessmentRepository, new ManeuverCandidateRepository(), null);
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

            // 1. Check if candidates are already generated in database
            try {
                List<ManeuverCandidate> existing =
                        maneuverCandidateRepository.findByEncounterId(encounterId);

                if (!existing.isEmpty()) {
                    for (ManeuverCandidate c : existing) {
                        candidates.add(candidateMap(c));
                    }
                    return ResponseEntity.ok(candidates);
                }
            } catch (Exception ignored) {
                // If DB check fails, proceed to real planner
            }

            // 2. Retrieve encounter details
            String runId = null;
            String objectAId = null;
            String objectBId = null;
            double minSeparationKm = 100.0;
            double timeToClosestApproachSeconds = 3600.0;
            double relativeVelocityKmPerSec = 0.0;
            int detectedAtTick = 0;

            com.orbitalnet.core.Encounter activeEncounter = null;
            if (simulationRunService != null) {
                activeEncounter = simulationRunService.getCoreEncounter(encounterId);
            }

            if (activeEncounter != null) {
                runId = activeEncounter.getRunId();
                objectAId = activeEncounter.getObjectAId();
                objectBId = activeEncounter.getObjectBId();
                minSeparationKm = activeEncounter.getMinSeparationKm();
                timeToClosestApproachSeconds = activeEncounter.getTimeToClosestApproachSeconds();
                relativeVelocityKmPerSec = activeEncounter.getRelativeVelocityKmPerSec();
                detectedAtTick = activeEncounter.getDetectedAtTick();
            } else {
                String encounterSql = """
                        SELECT run_id,
                               object_a_id,
                               object_b_id,
                               min_separation_km,
                               time_to_closest_approach_seconds,
                               relative_velocity_km_per_sec,
                               detected_at_tick
                        FROM encounter
                        WHERE encounter_id = ?
                        """;

                try (
                        Connection connection =
                                DriverManager.getConnection(
                                        url, username, password);
                        PreparedStatement statement =
                                connection.prepareStatement(encounterSql)
                ) {
                    statement.setString(1, encounterId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) {
                            runId = result.getString("run_id");
                            objectAId = result.getString("object_a_id");
                            objectBId = result.getString("object_b_id");
                            minSeparationKm = result.getDouble("min_separation_km");
                            timeToClosestApproachSeconds =
                                    result.getDouble("time_to_closest_approach_seconds");
                            relativeVelocityKmPerSec =
                                    result.getDouble("relative_velocity_km_per_sec");
                            detectedAtTick = result.getInt("detected_at_tick");
                        }
                    }
                }
            }

            if (objectAId == null || objectBId == null) {
                return ResponseEntity.notFound().build();
            }

            // 3. Retrieve space objects
            SpaceObject objA = null;
            SpaceObject objB = null;

            if (simulationRunService != null && runId != null) {
                List<SpaceObject> coreObjects = simulationRunService.getCoreObjects(runId);
                for (SpaceObject obj : coreObjects) {
                    if (obj.getId().equals(objectAId)) {
                        objA = obj;
                    } else if (obj.getId().equals(objectBId)) {
                        objB = obj;
                    }
                }
            }

            if (objA == null || objB == null) {
                String spaceObjectSql = """
                        SELECT object_id,
                               object_type,
                               name,
                               size,
                               status,
                               pos_x,
                               pos_y,
                               pos_z,
                               vel_x,
                               vel_y,
                               vel_z
                        FROM space_object
                        WHERE object_id IN (?, ?)
                        """;

                try (
                        Connection connection =
                                DriverManager.getConnection(
                                        url, username, password);
                        PreparedStatement statement =
                                connection.prepareStatement(spaceObjectSql)
                ) {
                    statement.setString(1, objectAId);
                    statement.setString(2, objectBId);

                    try (ResultSet rs = statement.executeQuery()) {
                        while (rs.next()) {
                            String id = rs.getString("object_id");
                            String type = rs.getString("object_type");
                            String name = rs.getString("name");
                            double size = rs.getDouble("size");
                            String statusStr = rs.getString("status");
                            SpaceObject.Status status = SpaceObject.Status.valueOf(statusStr);
                            Vector3D pos = new Vector3D(
                                    rs.getDouble("pos_x"),
                                    rs.getDouble("pos_y"),
                                    rs.getDouble("pos_z")
                            );
                            Vector3D vel = new Vector3D(
                                    rs.getDouble("vel_x"),
                                    rs.getDouble("vel_y"),
                                    rs.getDouble("vel_z")
                            );

                            SpaceObject constructed;
                            if ("SATELLITE".equalsIgnoreCase(type)) {
                                constructed = new Satellite(
                                        id,
                                        name != null ? name : id,
                                        pos,
                                        vel,
                                        size,
                                        status,
                                        null
                                );
                            } else {
                                constructed = new Debris(
                                        id,
                                        pos,
                                        vel,
                                        size,
                                        status
                                );
                            }

                            if (id.equals(objectAId)) {
                                objA = constructed;
                            } else if (id.equals(objectBId)) {
                                objB = constructed;
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            if (objA == null) {
                objA = new Satellite(
                        objectAId,
                        objectAId,
                        new Vector3D(-100.0, 0.0, 0.0),
                        new Vector3D(1.0, 0.0, 0.0),
                        0.01,
                        SpaceObject.Status.ACTIVE,
                        null
                );
            }

            if (objB == null) {
                objB = new Debris(
                        objectBId,
                        new Vector3D(100.0, 0.0, 0.0),
                        new Vector3D(-1.0, 0.0, 0.0),
                        0.001,
                        SpaceObject.Status.ACTIVE
                );
            }

            // 4. Maneuver rule: satellite must be the target object
            SpaceObject targetObj;
            SpaceObject otherObj;

            if (objA instanceof Satellite && !(objB instanceof Satellite)) {
                targetObj = objA;
                otherObj = objB;
            } else if (objB instanceof Satellite && !(objA instanceof Satellite)) {
                targetObj = objB;
                otherObj = objA;
            } else {
                targetObj = objB;
                otherObj = objA;
            }

            double validSep = Math.max(0.0, minSeparationKm);
            double validTime = Math.min(Math.max(0.0, timeToClosestApproachSeconds), 3600.0);
            double validRelVel = Math.max(0.0, relativeVelocityKmPerSec);
            int validTick = Math.max(0, detectedAtTick);
            String validRunId = (runId != null && !runId.isBlank()) ? runId : "DEFAULT-RUN";

            com.orbitalnet.core.Encounter coreEncounter =
                    new com.orbitalnet.core.Encounter(
                            encounterId,
                            validRunId,
                            objA.getId(),
                            objB.getId(),
                            validSep,
                            validTime,
                            validRelVel,
                            validTick
                    );

            // 5. Execute real core ManeuverPlanner
            ManeuverPlanner planner = new ManeuverPlanner();
            List<com.orbitalnet.core.ManeuverCandidate> coreCandidates =
                    planner.planManeuvers(coreEncounter, targetObj, otherObj);

            // 6. Persist generated candidates to database
            for (com.orbitalnet.core.ManeuverCandidate c : coreCandidates) {
                ManeuverCandidate dbCandidate =
                        new ManeuverCandidate(
                                c.getManeuverCandidateId(),
                                encounterId,
                                c.getTargetObjectId(),
                                c.getDeltaV().getX(),
                                c.getDeltaV().getY(),
                                c.getDeltaV().getZ(),
                                c.getNewMinSeparationKm(),
                                c.getCost(),
                                c.getRank()
                        );

                try {
                    maneuverCandidateRepository.save(dbCandidate);
                } catch (Exception ignored) {
                }
            }

            // 7. Read candidates back from database or map directly
            try {
                List<ManeuverCandidate> savedCandidates =
                        maneuverCandidateRepository.findByEncounterId(encounterId);

                if (!savedCandidates.isEmpty()) {
                    for (ManeuverCandidate c : savedCandidates) {
                        candidates.add(candidateMap(c));
                    }
                    return ResponseEntity.ok(candidates);
                }
            } catch (Exception ignored) {
            }

            for (com.orbitalnet.core.ManeuverCandidate c : coreCandidates) {
                Map<String, Object> map = new HashMap<>();
                map.put("maneuverCandidateId", c.getManeuverCandidateId());
                map.put("encounterId", c.getEncounterId());
                map.put("targetObjectId", c.getTargetObjectId());
                map.put("deltaV", Map.of(
                        "x", c.getDeltaV().getX(),
                        "y", c.getDeltaV().getY(),
                        "z", c.getDeltaV().getZ()
                ));
                map.put("newMinSeparationKm", c.getNewMinSeparationKm());
                map.put("cost", c.getCost());
                map.put("rank", c.getRank());
                candidates.add(map);
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

    private Map<String, Object> candidateMap(ManeuverCandidate candidate) {

        Map<String, Object> map = new HashMap<>();

        map.put("maneuverCandidateId", candidate.getManeuverCandidateId());
        map.put("encounterId", candidate.getEncounterId());
        map.put("targetObjectId", candidate.getTargetObjectId());
        map.put("deltaV", Map.of(
                "x", candidate.getDeltaVX(),
                "y", candidate.getDeltaVY(),
                "z", candidate.getDeltaVZ()
        ));
        map.put("newMinSeparationKm", candidate.getNewMinSeparationKm());
        map.put("cost", candidate.getCost());
        map.put("rank", candidate.getRank());

        return map;
    }

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
