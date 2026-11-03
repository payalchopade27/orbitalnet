package backend;

import backend.model.RiskAssessment;
import backend.model.SimulationRun;
import backend.repository.EncounterRepository;
import backend.repository.RiskAssessmentRepository;
import backend.repository.SimulationRunRepository;
import com.orbitalnet.core.EncounterDetectionService;
import com.orbitalnet.core.EncounterTracker;
import com.orbitalnet.core.InitialObjectState;
import com.orbitalnet.core.OrbitalEngine;
import com.orbitalnet.core.RiskEngine;
import com.orbitalnet.core.ScenarioGenerationConfig;
import com.orbitalnet.core.SpaceObject;
import com.orbitalnet.core.Vector3D;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class SimulationRunService {

    private final Map<String, Map<String, Object>> runs =
            new ConcurrentHashMap<>();

    private final Map<String, ActiveSession> sessions =
            new ConcurrentHashMap<>();

    private final ScenarioService scenarioService;
    private final SimulationRunRepository simulationRunRepository;
    private final EncounterRepository encounterRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;

    private final OrbitalEngine orbitalEngine = new OrbitalEngine();
    private final EncounterDetectionService detectionService = new EncounterDetectionService();
    private final RiskEngine riskEngine = new RiskEngine();

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

    private static class ActiveSession {
        final String runId;
        final String scenarioId;
        final com.orbitalnet.core.SimulationRun coreRun;
        final EncounterTracker tracker;
        final int durationSeconds;
        final Set<String> savedEncounterIds = ConcurrentHashMap.newKeySet();

        ActiveSession(
                String runId,
                String scenarioId,
                com.orbitalnet.core.SimulationRun coreRun,
                EncounterTracker tracker,
                int durationSeconds) {
            this.runId = runId;
            this.scenarioId = scenarioId;
            this.coreRun = coreRun;
            this.tracker = tracker;
            this.durationSeconds = durationSeconds;
        }
    }

    public Map<String, Object> createRun(String scenarioId) {

        ScenarioResponse scenario =
                scenarioService.getScenario(scenarioId);

        if (scenario == null) {
            return null;
        }

        String runId = UUID.randomUUID().toString();

        com.orbitalnet.core.Scenario coreScenario =
                createCoreScenario(scenario);

        com.orbitalnet.core.SimulationRun coreRun =
                new com.orbitalnet.core.SimulationRun(
                        runId,
                        coreScenario,
                        null,
                        null
                );

        EncounterTracker tracker = new EncounterTracker(runId);

        ActiveSession session = new ActiveSession(
                runId,
                scenarioId,
                coreRun,
                tracker,
                scenario.getDuration() * 60
        );

        sessions.put(runId, session);

        List<Map<String, Object>> objects =
                formatObjects(coreRun.getObjects());

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

    private com.orbitalnet.core.Scenario createCoreScenario(
            ScenarioResponse scenario) {

        String scenarioId = scenario.getScenarioId();
        long seed = (long) scenarioId.hashCode();

        if (scenario.getSatellites() == 1 && scenario.getDebris() == 1) {
            return orbitalEngine.generateKnownThreatScenario(scenarioId, seed);
        }

        ScenarioGenerationConfig config =
                new ScenarioGenerationConfig(
                        scenario.getSatellites(),
                        scenario.getDebris(),
                        -100.0,
                        100.0,
                        -1.0,
                        1.0,
                        0.001,
                        0.05
                );

        com.orbitalnet.core.Scenario generated =
                orbitalEngine.generateScenario(scenarioId, config, seed);

        if (scenario.getSatellites() >= 1 && scenario.getDebris() >= 1) {

            List<InitialObjectState> states =
                    new ArrayList<>(generated.getInitialObjectStates());

            int satIdx = -1;
            int debIdx = -1;

            for (int i = 0; i < states.size(); i++) {
                if (satIdx == -1
                        && states.get(i).getType() == SpaceObject.ObjectType.SATELLITE) {
                    satIdx = i;
                }
                if (debIdx == -1
                        && states.get(i).getType() == SpaceObject.ObjectType.DEBRIS) {
                    debIdx = i;
                }
            }

            if (satIdx != -1) {
                states.set(satIdx, new InitialObjectState(
                        "SAT-1",
                        SpaceObject.ObjectType.SATELLITE,
                        new Vector3D(-100.0, 0.0, 0.0),
                        new Vector3D(1.0, 0.0, 0.0),
                        0.01,
                        SpaceObject.Status.ACTIVE,
                        "Satellite-1",
                        null
                ));
            }

            if (debIdx != -1) {
                states.set(debIdx, new InitialObjectState(
                        "DEB-1",
                        SpaceObject.ObjectType.DEBRIS,
                        new Vector3D(100.0, 0.0, 0.0),
                        new Vector3D(-1.0, 0.0, 0.0),
                        0.001,
                        SpaceObject.Status.ACTIVE,
                        null,
                        null
                ));
            }

            return new com.orbitalnet.core.Scenario(
                    scenarioId,
                    config,
                    seed,
                    states,
                    com.orbitalnet.core.Scenario.Status.READY
            );
        }

        return generated;
    }

    private ActiveSession restoreSession(String runId) {

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

            if (scenario == null) {
                return null;
            }

            com.orbitalnet.core.Scenario coreScenario =
                    createCoreScenario(scenario);

            com.orbitalnet.core.SimulationRun coreRun =
                    new com.orbitalnet.core.SimulationRun(
                            runId,
                            coreScenario,
                            null,
                            null
                    );

            if (!"PENDING".equals(dbRun.getStatus())) {
                coreRun.start();

                for (int i = 0; i < dbRun.getCurrentTick(); i++) {
                    if (coreRun.getStatus()
                            == com.orbitalnet.core.SimulationRun.Status.RUNNING) {
                        coreRun.advanceTick();
                    }
                }
            }

            EncounterTracker tracker = new EncounterTracker(runId);

            ActiveSession session = new ActiveSession(
                    runId,
                    dbRun.getScenarioId(),
                    coreRun,
                    tracker,
                    scenario.getDuration() * 60
            );

            List<backend.model.Encounter> existingEncounters =
                    encounterRepository.findByRunId(runId);

            for (backend.model.Encounter enc : existingEncounters) {
                session.savedEncounterIds.add(enc.getEncounterId());
            }

            sessions.put(runId, session);

            return session;

        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, Object> getRun(String runId) {

        Map<String, Object> run =
                runs.get(runId);

        if (run != null) {
            return run;
        }

        ActiveSession session = sessions.get(runId);

        if (session == null) {
            session = restoreSession(runId);
        }

        if (session == null) {
            return null;
        }

        List<Map<String, Object>> objects =
                formatObjects(session.coreRun.getObjects());

        List<Map<String, Object>> encounters =
                formatEncounters(session.tracker.getEncounters());

        Map<String, Object> restoredRun =
                Map.of(
                        "runId", session.runId,
                        "scenarioId", session.scenarioId,
                        "status", session.coreRun.getStatus().name(),
                        "currentTick", session.coreRun.getCurrentTick(),
                        "elapsedTimeSeconds", session.coreRun.getElapsedTimeSeconds(),
                        "objects", objects,
                        "encounters", encounters
                );

        runs.put(runId, restoredRun);

        return restoredRun;
    }

    public Map<String, Object> advanceRun(String runId) {

        ActiveSession session = sessions.get(runId);

        if (session == null) {
            session = restoreSession(runId);
        }

        if (session == null) {
            return null;
        }

        if (session.coreRun.getStatus()
                == com.orbitalnet.core.SimulationRun.Status.COMPLETED) {
            return getRun(runId);
        }

        if (session.coreRun.getStatus()
                == com.orbitalnet.core.SimulationRun.Status.PENDING) {
            session.coreRun.start();
        }

        session.coreRun.advanceTick();

        int currentTick =
                session.coreRun.getCurrentTick();

        double elapsedTime =
                session.coreRun.getElapsedTimeSeconds();

        String status =
                session.coreRun.getStatus().name();

        if (elapsedTime >= session.durationSeconds
                && !"COMPLETED".equals(status)) {
            status = "COMPLETED";
        }

        List<EncounterDetectionService.DetectedEncounter> detections =
                detectionService.detect(session.coreRun.getObjects());

        List<com.orbitalnet.core.Encounter> updatedEncounters =
                session.tracker.processTick(
                        currentTick,
                        detections,
                        session.coreRun.getObjects()
                );

        for (com.orbitalnet.core.Encounter encounter : updatedEncounters) {
            if (encounter.getStatus()
                    == com.orbitalnet.core.Encounter.Status.DETECTED
                    || encounter.getRiskScore() == null) {

                com.orbitalnet.core.RiskScore score =
                        riskEngine.assess(encounter);

                encounter.assess(score);
            }
        }

        for (com.orbitalnet.core.Encounter encounter : session.tracker.getEncounters()) {
            if (session.savedEncounterIds.add(encounter.getEncounterId())) {

                try {

                    backend.model.Encounter dbEncounter =
                            new backend.model.Encounter(
                                    encounter.getEncounterId(),
                                    runId,
                                    encounter.getObjectAId(),
                                    encounter.getObjectBId(),
                                    encounter.getMinSeparationKm(),
                                    encounter.getTimeToClosestApproachSeconds(),
                                    encounter.getRelativeVelocityKmPerSec(),
                                    (long) encounter.getDetectedAtTick(),
                                    encounter.getStatus().name()
                            );

                    encounterRepository.save(dbEncounter);

                    com.orbitalnet.core.RiskScore risk =
                            encounter.getRiskScore();

                    if (risk == null) {
                        risk = riskEngine.assess(encounter);
                        encounter.assess(risk);
                    }

                    RiskAssessment dbRisk =
                            new RiskAssessment(
                                    encounter.getEncounterId(),
                                    risk.getTotalScore(),
                                    risk.getSeparationFactor(),
                                    risk.getVelocityFactor(),
                                    risk.getTimeFactor(),
                                    risk.getExplanation()
                            );

                    riskAssessmentRepository.save(dbRisk);

                } catch (Exception e) {

                    throw new RuntimeException(
                            "Failed to save encounter and risk",
                            e
                    );
                }
            }
        }

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

        List<Map<String, Object>> updatedObjects =
                formatObjects(session.coreRun.getObjects());

        List<Map<String, Object>> encounters =
                formatEncounters(session.tracker.getEncounters());

        Map<String, Object> updatedRun =
                Map.of(
                        "runId", runId,
                        "scenarioId", session.scenarioId,
                        "status", status,
                        "currentTick", currentTick,
                        "elapsedTimeSeconds", elapsedTime,
                        "objects", updatedObjects,
                        "encounters", encounters
                );

        runs.put(runId, updatedRun);

        return updatedRun;
    }

    private List<Map<String, Object>> formatObjects(
            List<SpaceObject> spaceObjects) {

        List<Map<String, Object>> list =
                new ArrayList<>();

        for (SpaceObject obj : spaceObjects) {

            Map<String, Double> position = Map.of(
                    "x", obj.getPosition().getX(),
                    "y", obj.getPosition().getY(),
                    "z", obj.getPosition().getZ()
            );

            Map<String, Double> velocity = Map.of(
                    "x", obj.getVelocity().getX(),
                    "y", obj.getVelocity().getY(),
                    "z", obj.getVelocity().getZ()
            );

            Map<String, Object> objectMap = Map.of(
                    "id", obj.getId(),
                    "type", obj.getType().name(),
                    "status", obj.getStatus().name(),
                    "size", obj.getSize(),
                    "position", position,
                    "velocity", velocity
            );

            list.add(objectMap);
        }

        return list;
    }

    private List<Map<String, Object>> formatEncounters(
            List<com.orbitalnet.core.Encounter> coreEncounters) {

        List<Map<String, Object>> list =
                new ArrayList<>();

        for (com.orbitalnet.core.Encounter enc : coreEncounters) {

            int score = enc.getRiskScore() != null
                    ? enc.getRiskScore().getTotalScore()
                    : 0;

            Map<String, Object> map = Map.of(
                    "encounterId", enc.getEncounterId(),
                    "objectAId", enc.getObjectAId(),
                    "objectBId", enc.getObjectBId(),
                    "minSeparationKm", enc.getMinSeparationKm(),
                    "timeToClosestApproachSeconds", enc.getTimeToClosestApproachSeconds(),
                    "relativeVelocityKmPerSec", enc.getRelativeVelocityKmPerSec(),
                    "riskScore", score,
                    "status", enc.getStatus().name(),
                    "detectedAtTick", (long) enc.getDetectedAtTick()
            );

            list.add(map);
        }

        return list;
    }

    public List<SpaceObject> getCoreObjects(String runId) {

        ActiveSession session = sessions.get(runId);

        if (session == null) {
            session = restoreSession(runId);
        }

        if (session == null) {
            return List.of();
        }

        return session.coreRun.getObjects();
    }

    public com.orbitalnet.core.Encounter getCoreEncounter(String encounterId) {

        for (ActiveSession session : sessions.values()) {
            for (com.orbitalnet.core.Encounter e : session.tracker.getEncounters()) {
                if (e.getEncounterId().equals(encounterId)) {
                    return e;
                }
            }
        }

        return null;
    }
}
