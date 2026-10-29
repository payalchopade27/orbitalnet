package backend;

import backend.model.Encounter;
import backend.model.RiskAssessment;
import backend.model.SimulationRun;
import backend.repository.EncounterRepository;
import backend.repository.RiskAssessmentRepository;
import backend.repository.SimulationRunRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class SimulationRunService {

    private final Map<String, Map<String, Object>> runs =
            new ConcurrentHashMap<>();

    private final ScenarioService scenarioService;
    private final SimulationRunRepository simulationRunRepository;
    private final EncounterRepository encounterRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;

    public SimulationRunService(
            ScenarioService scenarioService,
            SimulationRunRepository simulationRunRepository,
            EncounterRepository encounterRepository,
            RiskAssessmentRepository riskAssessmentRepository) {

        this.scenarioService = scenarioService;
        this.simulationRunRepository = simulationRunRepository;
        this.encounterRepository = encounterRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
    }

    public Map<String, Object> createRun(String scenarioId) {

        ScenarioResponse scenario =
                scenarioService.getScenario(scenarioId);

        if (scenario == null) {
            return null;
        }

        String runId = UUID.randomUUID().toString();

        List<Map<String, Object>> objects =
                generateObjects(scenario);

        Map<String, Object> run = Map.of(
                "runId", runId,
                "scenarioId", scenarioId,
                "status", "PENDING",
                "currentTick", 0,
                "elapsedTimeSeconds", 0.0,
                "objects", objects,
                "encounters", List.of()
        );

        runs.put(runId, run);

        try {

            SimulationRun dbRun = new SimulationRun(
                    runId,
                    scenarioId,
                    "PENDING",
                    0,
                    0.0,
                    "GRID",
                    null,
                    null
            );

            simulationRunRepository.save(dbRun);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to save simulation run",
                    e
            );
        }

        return run;
    }

    private List<Map<String, Object>> generateObjects(
            ScenarioResponse scenario) {

        List<Map<String, Object>> objects =
                new ArrayList<>();

        Random random =
                new Random(
                        scenario.getScenarioId().hashCode()
                );

        for (int i = 1;
             i <= scenario.getSatellites();
             i++) {

            objects.add(
                    createObject(
                   String.format("SAT-%03d", i),
                  "SATELLITE",
                   0.01,
                    random
                )
            );
        }

        for (int i = 1;
             i <= scenario.getDebris();
             i++) {

            objects.add(
                    createObject(
                    String.format("DEB-%03d", i),
                    "DEBRIS",
                    0.001,
                    random
)
            );
        }

        return objects;
    }

    private Map<String, Object> createObject(
        String id,
        String type,
        double size,
        Random random) {

    double x;
    double y;
    double z;

    double vx;
    double vy;
    double vz;

    if ("SAT-001".equals(id)) {

        x = 0.0;
        y = 0.0;
        z = 0.0;

        vx = 1.0;
        vy = 0.0;
        vz = 0.0;

    } else if ("DEB-001".equals(id)) {

        x = 50.0;
        y = 0.0;
        z = 0.0;

        vx = -1.0;
        vy = 0.0;
        vz = 0.0;

    } else {

        x = random.nextDouble() * 20000 - 10000;
        y = random.nextDouble() * 20000 - 10000;
        z = random.nextDouble() * 20000 - 10000;

        vx = random.nextDouble() * 15 - 7.5;
        vy = random.nextDouble() * 15 - 7.5;
        vz = random.nextDouble() * 15 - 7.5;
    }

    Map<String, Double> position = Map.of(
            "x", x,
            "y", y,
            "z", z
    );

    Map<String, Double> velocity = Map.of(
            "x", vx,
            "y", vy,
            "z", vz
    );

    return Map.of(
            "id", id,
            "type", type,
            "status", "ACTIVE",
            "size", size,
            "position", position,
            "velocity", velocity
    );
}
    public Map<String, Object> getRun(String runId) {

        Map<String, Object> run =
                runs.get(runId);

        if (run != null) {
            return run;
        }

        try {

            SimulationRun dbRun =
                    simulationRunRepository.findById(runId);

            if (dbRun == null) {
                return null;
            }

            ScenarioResponse scenario =
                    scenarioService.getScenario(
                            dbRun.getScenarioId()
                    );

            List<Map<String, Object>> objects =
                    scenario == null
                            ? new ArrayList<>()
                            : generateObjects(scenario);

            Map<String, Object> restoredRun =
                    Map.of(
                            "runId", dbRun.getRunId(),
                            "scenarioId", dbRun.getScenarioId(),
                            "status", dbRun.getStatus(),
                            "currentTick",
                                    dbRun.getCurrentTick(),
                            "elapsedTimeSeconds",
                                    dbRun.getElapsedTimeSeconds(),
                            "objects", objects,
                            "encounters",
                                    List.of()
                    );

            runs.put(runId, restoredRun);

            return restoredRun;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to get simulation run",
                    e
            );
        }
    }

    public Map<String, Object> advanceRun(String runId) {

        Map<String, Object> run =
                runs.get(runId);

        if (run == null) {
            run = getRun(runId);
        }

        if (run == null) {
            return null;
        }

        if ("COMPLETED".equals(run.get("status"))) {
            return run;
        }

        List<Map<String, Object>> updatedObjects =
                new ArrayList<>();

        List<Map<String, Object>> objects =
                (List<Map<String, Object>>) run.get("objects");

        for (Map<String, Object> object : objects) {

            Map<String, Double> oldPosition =
                    (Map<String, Double>)
                            object.get("position");

            Map<String, Double> velocity =
                    (Map<String, Double>)
                            object.get("velocity");

            Map<String, Double> newPosition =
                    Map.of(
                            "x",
                            oldPosition.get("x")
                                    + velocity.get("x") * 5.0,

                            "y",
                            oldPosition.get("y")
                                    + velocity.get("y") * 5.0,

                            "z",
                            oldPosition.get("z")
                                    + velocity.get("z") * 5.0
                    );

            Map<String, Object> updatedObject =
                    Map.of(
                            "id", object.get("id"),
                            "type", object.get("type"),
                            "status", object.get("status"),
                            "size", object.get("size"),
                            "position", newPosition,
                            "velocity", velocity
                    );

            updatedObjects.add(updatedObject);
        }

        int currentTick =
                ((Number) run.get("currentTick"))
                        .intValue() + 1;

        double elapsedTime =
                currentTick * 5.0;

        ScenarioResponse scenario =
                scenarioService.getScenario(
                        (String) run.get("scenarioId")
                );

        int durationSeconds =
                scenario.getDuration() * 60;

        String status =
                elapsedTime >= durationSeconds
                        ? "COMPLETED"
                        : "RUNNING";

        List<Map<String, Object>> encounters =
                detectEncounters(
                        updatedObjects,
                        currentTick,
                        runId
                );

        Map<String, Object> updatedRun =
                Map.of(
                        "runId", run.get("runId"),
                        "scenarioId",
                        run.get("scenarioId"),
                        "status", status,
                        "currentTick", currentTick,
                        "elapsedTimeSeconds",
                        elapsedTime,
                        "objects", updatedObjects,
                        "encounters", encounters
                );

        runs.put(runId, updatedRun);

        try {

            simulationRunRepository.updateProgress(
                    runId,
                    status,
                    currentTick,
                    elapsedTime
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to update simulation run",
                    e
            );
        }

        return updatedRun;
    }

    private List<Map<String, Object>> detectEncounters(
            List<Map<String, Object>> objects,
            int currentTick,
            String runId) {

        List<Map<String, Object>> encounters =
                new ArrayList<>();

        double detectionDistanceKm = 100.0;
        double horizonSeconds = 3600.0;

        for (int i = 0;
             i < objects.size();
             i++) {

            for (int j = i + 1;
                 j < objects.size();
                 j++) {

                Map<String, Object> a =
                        objects.get(i);

                Map<String, Object> b =
                        objects.get(j);

                Map<String, Double> posA =
                        (Map<String, Double>)
                                a.get("position");

                Map<String, Double> posB =
                        (Map<String, Double>)
                                b.get("position");

                Map<String, Double> velA =
                        (Map<String, Double>)
                                a.get("velocity");

                Map<String, Double> velB =
                        (Map<String, Double>)
                                b.get("velocity");

                double rx =
                        posB.get("x") - posA.get("x");

                double ry =
                        posB.get("y") - posA.get("y");

                double rz =
                        posB.get("z") - posA.get("z");

                double vx =
                        velB.get("x") - velA.get("x");

                double vy =
                        velB.get("y") - velA.get("y");

                double vz =
                        velB.get("z") - velA.get("z");

                double velocitySquared =
                        vx * vx
                        + vy * vy
                        + vz * vz;

                double timeToClosestApproach = 0.0;

                if (velocitySquared > 0.0) {

                    timeToClosestApproach =
                            -(rx * vx
                            + ry * vy
                            + rz * vz)
                            / velocitySquared;
                }

                timeToClosestApproach =
                        Math.max(
                                0.0,
                                Math.min(
                                        horizonSeconds,
                                        timeToClosestApproach
                                )
                        );

                double closestX =
                        rx
                        + vx * timeToClosestApproach;

                double closestY =
                        ry
                        + vy * timeToClosestApproach;

                double closestZ =
                        rz
                        + vz * timeToClosestApproach;

                double minSeparationKm =
                        Math.sqrt(
                                closestX * closestX
                                + closestY * closestY
                                + closestZ * closestZ
                        );

                if (minSeparationKm <=
                        detectionDistanceKm) {

                    double relativeVelocity =
                            Math.sqrt(
                                    velocitySquared
                            );

                    double separationFactor =
                            Math.max(
                                    0.0,
                                    Math.min(
                                            1.0,
                                            1.0
                                            - minSeparationKm
                                            / 100.0
                                    )
                            );

                    double velocityFactor =
                            Math.max(
                                    0.0,
                                    Math.min(
                                            1.0,
                                            relativeVelocity
                                            / 15.0
                                    )
                            );

                    double timeFactor =
                            1.0
                            - Math.max(
                                    0.0,
                                    Math.min(
                                            1.0,
                                            timeToClosestApproach
                                            / horizonSeconds
                                    )
                            );

                    int riskScore =
                            (int) Math.round(
                                    100.0 * (
                                            0.50
                                            * separationFactor
                                            + 0.30
                                            * velocityFactor
                                            + 0.20
                                            * timeFactor
                                    )
                            );

                    String objectAId =
                            (String) a.get("id");

                    String objectBId =
                            (String) b.get("id");

                    String existingEncounterId =
                            findExistingEncounterId(
                                    runId,
                                    objectAId,
                                    objectBId
                            );

                    if (existingEncounterId == null) {

                        String encounterId =
                                UUID.randomUUID()
                                        .toString();

                        Encounter encounter =
                                new Encounter(
                                        encounterId,
                                        runId,
                                        objectAId,
                                        objectBId,
                                        minSeparationKm,
                                        timeToClosestApproach,
                                        relativeVelocity,
                                        currentTick,
                                        "DETECTED"
                                );

                        RiskAssessment risk =
                                new RiskAssessment(
                                        encounterId,
                                        riskScore,
                                        separationFactor,
                                        velocityFactor,
                                        timeFactor,
                                        createRiskExplanation(
                                                riskScore
                                        )
                                );

                        try {

                            encounterRepository.save(
                                    encounter
                            );

                            riskAssessmentRepository.save(
                                    risk
                            );

                        } catch (Exception e) {

                            throw new RuntimeException(
                                    "Failed to save encounter and risk",
                                    e
                            );
                        }

                        encounters.add(
                                createEncounterResponse(
                                        encounter,
                                        risk
                                )
                        );

                    } else {

                        encounters.add(
                                createExistingEncounterResponse(
                                        existingEncounterId,
                                        runId,
                                        objectAId,
                                        objectBId,
                                        minSeparationKm,
                                        timeToClosestApproach,
                                        relativeVelocity,
                                        riskScore,
                                        currentTick
                                )
                        );
                    }
                }
            }
        }

        return encounters;
    }

    private String findExistingEncounterId(
            String runId,
            String objectAId,
            String objectBId) {

        try {

            List<Encounter> existing =
                    encounterRepository.findByRunId(runId);

            for (Encounter encounter : existing) {

                boolean sameOrder =
                        encounter.getObjectAId()
                                .equals(objectAId)
                        && encounter.getObjectBId()
                                .equals(objectBId);

                boolean reverseOrder =
                        encounter.getObjectAId()
                                .equals(objectBId)
                        && encounter.getObjectBId()
                                .equals(objectAId);

                if (sameOrder || reverseOrder) {
                    return encounter.getEncounterId();
                }
            }

            return null;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to check existing encounters",
                    e
            );
        }
    }

    private Map<String, Object> createEncounterResponse(
            Encounter encounter,
            RiskAssessment risk) {

        return Map.of(
                "encounterId",
                encounter.getEncounterId(),

                "objectAId",
                encounter.getObjectAId(),

                "objectBId",
                encounter.getObjectBId(),

                "minSeparationKm",
                encounter.getMinSeparationKm(),

                "timeToClosestApproachSeconds",
                encounter.getTimeToClosestApproachSeconds(),

                "relativeVelocityKmPerSec",
                encounter.getRelativeVelocityKmPerSec(),

                "riskScore",
                risk.getTotalScore(),

                "status",
                encounter.getStatus(),

                "detectedAtTick",
                encounter.getDetectedAtTick()
        );
    }

    private Map<String, Object> createExistingEncounterResponse(
            String encounterId,
            String runId,
            String objectAId,
            String objectBId,
            double minSeparationKm,
            double timeToClosestApproach,
            double relativeVelocity,
            int riskScore,
            int currentTick) {

        return Map.of(
                "encounterId",
                encounterId,

                "objectAId",
                objectAId,

                "objectBId",
                objectBId,

                "minSeparationKm",
                minSeparationKm,

                "timeToClosestApproachSeconds",
                timeToClosestApproach,

                "relativeVelocityKmPerSec",
                relativeVelocity,

                "riskScore",
                riskScore,

                "status",
                "DETECTED",

                "detectedAtTick",
                currentTick
        );
    }

    private String createRiskExplanation(int riskScore) {

        if (riskScore >= 75) {
            return "Critical risk";
        }

        if (riskScore >= 50) {
            return "High risk";
        }

        if (riskScore >= 25) {
            return "Moderate risk";
        }

        return "Low risk";
    }
}