
package backend;

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

    public SimulationRunService(ScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    public Map<String, Object> createRun(String scenarioId) {

        ScenarioResponse scenario =
                scenarioService.getScenario(scenarioId);

        if (scenario == null) {
            return null;
        }

        String runId = UUID.randomUUID().toString();

        // Generate initial satellite and debris objects
        List<Map<String, Object>> objects =
                generateObjects(scenario);

        Map<String, Object> run = Map.of(
                "runId", runId,
                "scenarioId", scenarioId,
                "status", "PENDING",
                "currentTick", 0,
                "elapsedTimeSeconds", 0,
                "objects", objects,
                "encounters", List.of()
        );

        runs.put(runId, run);

        return run;
    }

    private List<Map<String, Object>> generateObjects(
            ScenarioResponse scenario) {

        List<Map<String, Object>> objects = new ArrayList<>();

        // Same scenario ID produces the same initial objects
        Random random = new Random(
                scenario.getScenarioId().hashCode()
        );

        // Generate satellites
        for (int i = 1; i <= scenario.getSatellites(); i++) {
            objects.add(createObject(
                    "SAT-" + i,
                    "SATELLITE",
                    0.01,
                    random
            ));
        }

        // Generate debris
        for (int i = 1; i <= scenario.getDebris(); i++) {
            objects.add(createObject(
                    "DEB-" + i,
                    "DEBRIS",
                    0.001,
                    random
            ));
        }

        return objects;
    }

    private Map<String, Object> createObject(
            String id,
            String type,
            double size,
            Random random) {

        Map<String, Double> position = Map.of(
                "x", random.nextDouble() * 20000 - 10000,
                "y", random.nextDouble() * 20000 - 10000,
                "z", random.nextDouble() * 20000 - 10000
        );

        Map<String, Double> velocity = Map.of(
                "x", random.nextDouble() * 15 - 7.5,
                "y", random.nextDouble() * 15 - 7.5,
                "z", random.nextDouble() * 15 - 7.5
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
            return runs.get(runId);
        }

        public Map<String, Object> advanceRun(String runId) {

        Map<String, Object> run = runs.get(runId);

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

        // Move every satellite and debris object
        for (Map<String, Object> object : objects) {

            Map<String, Double> oldPosition =
                    (Map<String, Double>) object.get("position");

            Map<String, Double> velocity =
                    (Map<String, Double>) object.get("velocity");

            Map<String, Double> newPosition = Map.of(
                    "x", oldPosition.get("x")
                            + velocity.get("x") * 5.0,
                    "y", oldPosition.get("y")
                            + velocity.get("y") * 5.0,
                    "z", oldPosition.get("z")
                            + velocity.get("z") * 5.0
            );

            Map<String, Object> updatedObject = Map.of(
                    "id", object.get("id"),
                    "type", object.get("type"),
                    "status", object.get("status"),
                    "size", object.get("size"),
                    "position", newPosition,
                    "velocity", velocity
            );

            updatedObjects.add(updatedObject);
        }

        
        
        int currentTick = (int) run.get("currentTick") + 1;
        int elapsedTime = currentTick * 5;

        // Get the scenario to check its configured duration
        ScenarioResponse scenario = scenarioService.getScenario(
                (String) run.get("scenarioId")
        );

        int durationSeconds = scenario.getDuration() * 60;

        String status = elapsedTime >= durationSeconds
                ? "COMPLETED"
                : "RUNNING";


        // NEW: Detect encounters using the updated object positions
        List<Map<String, Object>> encounters =
                detectEncounters(updatedObjects, currentTick);

        Map<String, Object> updatedRun = Map.of(
                "runId", run.get("runId"),
                "scenarioId", run.get("scenarioId"),
                "status", status,
                "currentTick", currentTick,
                "elapsedTimeSeconds", elapsedTime,
                "objects", updatedObjects,
                "encounters", encounters
        );

        runs.put(runId, updatedRun);

        return updatedRun;
    }

    
    private List<Map<String, Object>> detectEncounters(
            List<Map<String, Object>> objects,
            int currentTick) {

        List<Map<String, Object>> encounters = new ArrayList<>();

        double detectionDistanceKm = 5000.0;
        double horizonSeconds = 3600.0;

        for (int i = 0; i < objects.size(); i++) {
            for (int j = i + 1; j < objects.size(); j++) {

                Map<String, Object> a = objects.get(i);
                Map<String, Object> b = objects.get(j);

                Map<String, Double> posA =
                        (Map<String, Double>) a.get("position");
                Map<String, Double> posB =
                        (Map<String, Double>) b.get("position");

                Map<String, Double> velA =
                        (Map<String, Double>) a.get("velocity");
                Map<String, Double> velB =
                        (Map<String, Double>) b.get("velocity");

                double rx = posB.get("x") - posA.get("x");
                double ry = posB.get("y") - posA.get("y");
                double rz = posB.get("z") - posA.get("z");

                double vx = velB.get("x") - velA.get("x");
                double vy = velB.get("y") - velA.get("y");
                double vz = velB.get("z") - velA.get("z");

                double velocitySquared = vx * vx + vy * vy + vz * vz;

                double timeToClosestApproach = 0.0;

                if (velocitySquared > 0.0) {
                    timeToClosestApproach =
                            -(rx * vx + ry * vy + rz * vz)
                            / velocitySquared;
                }

                timeToClosestApproach = Math.max(
                        0.0,
                        Math.min(horizonSeconds, timeToClosestApproach)
                );

                double closestX = rx + vx * timeToClosestApproach;
                double closestY = ry + vy * timeToClosestApproach;
                double closestZ = rz + vz * timeToClosestApproach;

                double minSeparationKm = Math.sqrt(
                        closestX * closestX
                        + closestY * closestY
                        + closestZ * closestZ
                );

                
                if (minSeparationKm <= detectionDistanceKm) {

                    double relativeVelocity = Math.sqrt(velocitySquared);

                    double separationFactor = Math.max(
                            0.0,
                            Math.min(1.0, 1.0 - minSeparationKm / 100.0)
                    );

                    double velocityFactor = Math.max(
                            0.0,
                            Math.min(1.0, relativeVelocity / 15.0)
                    );

                    double timeFactor = 1.0 - Math.max(
                            0.0,
                            Math.min(1.0, timeToClosestApproach / horizonSeconds)
                    );

                    int riskScore = (int) Math.round(
                            100.0 * (
                                    0.50 * separationFactor
                                    + 0.30 * velocityFactor
                                    + 0.20 * timeFactor
                            )
                    );

                    encounters.add(Map.of(
                            "encounterId", UUID.randomUUID().toString(),
                            "objectAId", a.get("id"),
                            "objectBId", b.get("id"),
                            "minSeparationKm", minSeparationKm,
                            "timeToClosestApproachSeconds",
                                    timeToClosestApproach,
                            "relativeVelocityKmPerSec", relativeVelocity,
                            "riskScore", riskScore,
                            "status", "DETECTED",
                            "detectedAtTick", currentTick
                    ));
                }

            }
        }

        return encounters;
    }

}
