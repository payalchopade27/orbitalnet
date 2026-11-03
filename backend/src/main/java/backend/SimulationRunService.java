package backend;

import backend.model.RiskAssessment;
import backend.model.SimulationRun;
import backend.repository.EncounterRepository;
import backend.repository.RiskAssessmentRepository;
import backend.repository.SimulationRunRepository;
import backend.repository.SpaceObjectRepository;
import com.orbitalnet.core.EncounterDetectionService;
import com.orbitalnet.core.EncounterTracker;
import com.orbitalnet.core.InitialObjectState;
import com.orbitalnet.core.OrbitalEngine;
import com.orbitalnet.core.RiskEngine;
import com.orbitalnet.core.ScenarioGenerationConfig;
import com.orbitalnet.core.SpaceObject;
import com.orbitalnet.core.Vector3D;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
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
    private final SpaceObjectRepository spaceObjectRepository;

    private final OrbitalEngine orbitalEngine = new OrbitalEngine();
    private final EncounterDetectionService detectionService = new EncounterDetectionService();
    private final RiskEngine riskEngine = new RiskEngine();

    @Autowired
    public SimulationRunService(
            ScenarioService scenarioService,
            SimulationRunRepository simulationRunRepository,
            EncounterRepository encounterRepository,
            RiskAssessmentRepository riskAssessmentRepository,
            SpaceObjectRepository spaceObjectRepository) {

        this.scenarioService = scenarioService;
        this.simulationRunRepository = simulationRunRepository;
        this.encounterRepository = encounterRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.spaceObjectRepository = spaceObjectRepository;
    }

    private static class ActiveSession {
        final String runId;
        final String scenarioId;
        final com.orbitalnet.core.SimulationRun coreRun;
        final EncounterTracker tracker;
        final int durationSeconds;
        final Set<String> savedEncounterIds = ConcurrentHashMap.newKeySet();
        final Map<String, Map<String, Object>> restoredEncounters = new ConcurrentHashMap<>();

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

        ensureSpaceObjectsPersisted(scenarioId, coreScenario);

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

        // 1. Check for existing persisted space objects for this scenario; do not swallow exceptions
        List<backend.model.SpaceObject> existingDbObjects;
        if (spaceObjectRepository != null) {
            try {
                existingDbObjects = spaceObjectRepository.findByScenarioId(scenarioId);
            } catch (Exception e) {
                throw new RuntimeException("Failed to check existing space objects for scenario: " + scenarioId, e);
            }
        } else {
            existingDbObjects = List.of();
        }

        // 2. Generate expected initial states from core engine
        com.orbitalnet.core.Scenario generated;
        ScenarioGenerationConfig config;

        if (scenario.getSatellites() == 1 && scenario.getDebris() == 1) {
            generated = orbitalEngine.generateKnownThreatScenario(scenarioId, seed);
            config = generated.getGenerationConfig();
        } else {
            config = new ScenarioGenerationConfig(
                    scenario.getSatellites(),
                    scenario.getDebris(),
                    -100.0,
                    100.0,
                    -1.0,
                    1.0,
                    0.001,
                    0.05
            );
            generated = orbitalEngine.generateScenario(scenarioId, config, seed);
        }

        List<InitialObjectState> expectedStates = new ArrayList<>(generated.getInitialObjectStates());

        // Apply head-on collision scenario override if scenario has at least 1 sat and 1 deb (and not known threat)
        if (scenario.getSatellites() >= 1 && scenario.getDebris() >= 1
                && !(scenario.getSatellites() == 1 && scenario.getDebris() == 1)) {
            int satIdx = -1;
            int debIdx = -1;

            for (int i = 0; i < expectedStates.size(); i++) {
                if (satIdx == -1 && expectedStates.get(i).getType() == SpaceObject.ObjectType.SATELLITE) {
                    satIdx = i;
                }
                if (debIdx == -1 && expectedStates.get(i).getType() == SpaceObject.ObjectType.DEBRIS) {
                    debIdx = i;
                }
            }

            if (satIdx != -1) {
                expectedStates.set(satIdx, new InitialObjectState(
                        expectedStates.get(satIdx).getId(),
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
                expectedStates.set(debIdx, new InitialObjectState(
                        expectedStates.get(debIdx).getId(),
                        SpaceObject.ObjectType.DEBRIS,
                        new Vector3D(100.0, 0.0, 0.0),
                        new Vector3D(-1.0, 0.0, 0.0),
                        0.001,
                        SpaceObject.Status.ACTIVE,
                        null,
                        null
                ));
            }
        }

        // 3. Reconcile with existing DB objects or assign new full scenario-scoped IDs
        List<InitialObjectState> mappedStates = new ArrayList<>(expectedStates.size());

        if (existingDbObjects != null && !existingDbObjects.isEmpty()) {
            // Validate that all existing DB records belong strictly to this scenario
            for (backend.model.SpaceObject obj : existingDbObjects) {
                if (!scenarioId.equals(obj.getScenarioId())) {
                    throw new IllegalStateException(String.format(
                            "Cross-scenario mismatch: space_object %s has scenario_id %s, expected %s",
                            obj.getObjectId(), obj.getScenarioId(), scenarioId
                    ));
                }
            }

            // Validate total count matches expected
            if (existingDbObjects.size() != expectedStates.size()) {
                throw new IllegalStateException(String.format(
                        "Existing space objects count mismatch for scenario %s: expected %d, found %d in database",
                        scenarioId, expectedStates.size(), existingDbObjects.size()
                ));
            }

            // Separate existing DB objects by type
            List<backend.model.SpaceObject> existingSats = new ArrayList<>();
            List<backend.model.SpaceObject> existingDebs = new ArrayList<>();
            for (backend.model.SpaceObject obj : existingDbObjects) {
                if ("SATELLITE".equalsIgnoreCase(obj.getObjectType())) {
                    existingSats.add(obj);
                } else if ("DEBRIS".equalsIgnoreCase(obj.getObjectType())) {
                    existingDebs.add(obj);
                } else {
                    throw new IllegalStateException("Unknown space object type: " + obj.getObjectType());
                }
            }

            if (existingSats.size() != scenario.getSatellites() || existingDebs.size() != scenario.getDebris()) {
                throw new IllegalStateException(String.format(
                        "Existing space objects type distribution mismatch for scenario %s: expected %d sats, %d debs, found %d sats, %d debs",
                        scenarioId, scenario.getSatellites(), scenario.getDebris(), existingSats.size(), existingDebs.size()
                ));
            }

            // Deterministic matching: match each expected object to a corresponding existing DB record
            Set<String> matchedDbIds = new HashSet<>();

            for (InitialObjectState expected : expectedStates) {
                backend.model.SpaceObject matched = null;
                List<backend.model.SpaceObject> candidates = expected.getType() == SpaceObject.ObjectType.SATELLITE
                        ? existingSats
                        : existingDebs;

                // Priority 1: Match by name if non-null and matching, AND validate physics
                if (expected.getName() != null && !expected.getName().isBlank()) {
                    for (backend.model.SpaceObject candidate : candidates) {
                        if (!matchedDbIds.contains(candidate.getObjectId())
                                && expected.getName().equals(candidate.getName())) {
                            boolean physicsMatch = Math.abs(candidate.getPosX() - expected.getPosition().getX()) < 0.05
                                    && Math.abs(candidate.getPosY() - expected.getPosition().getY()) < 0.05
                                    && Math.abs(candidate.getPosZ() - expected.getPosition().getZ()) < 0.05
                                    && Math.abs(candidate.getVelX() - expected.getVelocity().getX()) < 0.05
                                    && Math.abs(candidate.getVelY() - expected.getVelocity().getY()) < 0.05
                                    && Math.abs(candidate.getVelZ() - expected.getVelocity().getZ()) < 0.05;
                            if (physicsMatch) {
                                matched = candidate;
                                break;
                            }
                        }
                    }
                }

                // Priority 2: Match by initial coordinates within tolerance
                if (matched == null) {
                    for (backend.model.SpaceObject candidate : candidates) {
                        if (!matchedDbIds.contains(candidate.getObjectId())
                                && Math.abs(candidate.getPosX() - expected.getPosition().getX()) < 0.05
                                && Math.abs(candidate.getPosY() - expected.getPosition().getY()) < 0.05
                                && Math.abs(candidate.getPosZ() - expected.getPosition().getZ()) < 0.05
                                && Math.abs(candidate.getVelX() - expected.getVelocity().getX()) < 0.05
                                && Math.abs(candidate.getVelY() - expected.getVelocity().getY()) < 0.05
                                && Math.abs(candidate.getVelZ() - expected.getVelocity().getZ()) < 0.05) {
                            matched = candidate;
                            break;
                        }
                    }
                }

                if (matched == null) {
                    throw new IllegalStateException(String.format(
                            "Cannot safely reconcile existing space object for scenario %s (type=%s, name=%s, pos=%s). State mismatch detected.",
                            scenarioId, expected.getType(), expected.getName(), expected.getPosition()
                    ));
                }

                matchedDbIds.add(matched.getObjectId());

                mappedStates.add(new InitialObjectState(
                        matched.getObjectId(), // Retain exact existing ID
                        expected.getType(),
                        expected.getPosition(),
                        expected.getVelocity(),
                        expected.getSize(),
                        expected.getStatus(),
                        expected.getName(),
                        expected.getOrbit()
                ));
            }

        } else {
            // New scenario: assign full scenario-scoped IDs: SAT-<scenarioId>-<N> and DEB-<scenarioId>-<N>
            int satCount = 1;
            int debCount = 1;

            for (InitialObjectState expected : expectedStates) {
                String generatedId;
                if (expected.getType() == SpaceObject.ObjectType.SATELLITE) {
                    generatedId = String.format("SAT-%s-%d", scenarioId, satCount++);
                } else {
                    generatedId = String.format("DEB-%s-%d", scenarioId, debCount++);
                }

                mappedStates.add(new InitialObjectState(
                        generatedId,
                        expected.getType(),
                        expected.getPosition(),
                        expected.getVelocity(),
                        expected.getSize(),
                        expected.getStatus(),
                        expected.getName(),
                        expected.getOrbit()
                ));
            }
        }

        return new com.orbitalnet.core.Scenario(
                scenarioId,
                config,
                seed,
                mappedStates,
                com.orbitalnet.core.Scenario.Status.READY
        );
    }

    private void ensureSpaceObjectsPersisted(
            String scenarioId,
            com.orbitalnet.core.Scenario coreScenario) {

        if (scenarioId == null || coreScenario == null || spaceObjectRepository == null) {
            return;
        }

        try {
            List<backend.model.SpaceObject> existing =
                    spaceObjectRepository.findByScenarioId(scenarioId);

            Set<String> existingIds = new HashSet<>();
            if (existing != null) {
                for (backend.model.SpaceObject obj : existing) {
                    if (scenarioId.equals(obj.getScenarioId())) {
                        existingIds.add(obj.getObjectId());
                    }
                }
            }

            List<backend.model.SpaceObject> toSave = new ArrayList<>();

            for (InitialObjectState state : coreScenario.getInitialObjectStates()) {
                if (existingIds.contains(state.getId())) {
                    continue;
                }

                Double altitudeKm = state.getOrbit() != null
                        ? state.getOrbit().getAltitudeKm()
                        : null;
                Double inclinationDeg = state.getOrbit() != null
                        ? state.getOrbit().getInclinationDeg()
                        : null;
                Double periodSeconds = state.getOrbit() != null
                        ? state.getOrbit().getPeriodSeconds()
                        : null;

                backend.model.SpaceObject dbObj = new backend.model.SpaceObject(
                        state.getId(),
                        scenarioId,
                        state.getType().name(),
                        state.getName(),
                        null,
                        state.getSize(),
                        state.getStatus().name(),
                        state.getPosition().getX(),
                        state.getPosition().getY(),
                        state.getPosition().getZ(),
                        state.getVelocity().getX(),
                        state.getVelocity().getY(),
                        state.getVelocity().getZ(),
                        altitudeKm,
                        inclinationDeg,
                        periodSeconds
                );

                toSave.add(dbObj);
            }

            if (!toSave.isEmpty()) {
                spaceObjectRepository.saveAll(toSave);
            }

            // Post-persistence verification: verify every expected object exists in DB for this scenario
            List<backend.model.SpaceObject> verified =
                    spaceObjectRepository.findByScenarioId(scenarioId);

            int expectedCount = coreScenario.getInitialObjectStates().size();
            int actualCount = verified != null ? verified.size() : 0;
            if (actualCount != expectedCount) {
                throw new IllegalStateException(String.format(
                        "Persistence verification failed for scenario %s: expected %d space objects in database, but found %d. Possible cross-scenario ID collision or write failure.",
                        scenarioId, expectedCount, actualCount
                ));
            }

            Set<String> verifiedIds = new HashSet<>();
            for (backend.model.SpaceObject obj : verified) {
                if (!scenarioId.equals(obj.getScenarioId())) {
                    throw new IllegalStateException(String.format(
                            "Cross-scenario corruption detected: space_object %s belongs to scenario %s, expected %s.",
                            obj.getObjectId(), obj.getScenarioId(), scenarioId
                    ));
                }
                verifiedIds.add(obj.getObjectId());
            }

            for (InitialObjectState state : coreScenario.getInitialObjectStates()) {
                if (!verifiedIds.contains(state.getId())) {
                    throw new IllegalStateException(String.format(
                            "Space object %s for scenario %s is missing from database after persistence.",
                            state.getId(), scenarioId
                    ));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to persist space objects for scenario: " + scenarioId,
                    e
            );
        }
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

                int score = 0;
                try {
                    RiskAssessment risk =
                            riskAssessmentRepository.findByEncounterId(enc.getEncounterId());
                    if (risk != null) {
                        score = risk.getTotalScore();
                    }
                } catch (Exception ignored) {
                }

                String encStatus = enc.getStatus() != null && !enc.getStatus().isBlank()
                        ? enc.getStatus()
                        : "DETECTED";

                String objA = enc.getObjectAId() != null ? enc.getObjectAId() : "";
                String objB = enc.getObjectBId() != null ? enc.getObjectBId() : "";

                Map<String, Object> encMap = Map.of(
                        "encounterId", enc.getEncounterId(),
                        "objectAId", objA,
                        "objectBId", objB,
                        "minSeparationKm", enc.getMinSeparationKm(),
                        "timeToClosestApproachSeconds", enc.getTimeToClosestApproachSeconds(),
                        "relativeVelocityKmPerSec", enc.getRelativeVelocityKmPerSec(),
                        "riskScore", score,
                        "status", encStatus,
                        "detectedAtTick", enc.getDetectedAtTick()
                );

                session.restoredEncounters.put(enc.getEncounterId(), encMap);
            }

            sessions.put(runId, session);

            return session;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to restore simulation run",
                    e
            );
        }
    }

    public Map<String, Object> getRun(String runId) {

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
                getMergedEncounters(session);

        Map<String, Object> restoredRun =
                Map.of(
                        "runId", runId,
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

        ensureSpaceObjectsPersisted(session.scenarioId, session.coreRun.getScenario());

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
                getMergedEncounters(session);

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

    private List<Map<String, Object>> getMergedEncounters(ActiveSession session) {

        Map<String, Map<String, Object>> merged = new LinkedHashMap<>();

        merged.putAll(session.restoredEncounters);

        List<Map<String, Object>> trackerEncounters =
                formatEncounters(session.tracker.getEncounters());

        for (Map<String, Object> encMap : trackerEncounters) {
            String encounterId = (String) encMap.get("encounterId");
            if (encounterId != null) {
                merged.put(encounterId, encMap);
            }
        }

        return new ArrayList<>(merged.values());
    }

    private List<Map<String, Object>> formatEncounters(
            List<com.orbitalnet.core.Encounter> coreEncounters) {

        List<Map<String, Object>> list =
                new ArrayList<>();

        for (com.orbitalnet.core.Encounter encounter : coreEncounters) {

            int riskScore = 0;

            if (encounter.getRiskScore() != null) {
                riskScore = encounter.getRiskScore().getTotalScore();
            }

            Map<String, Object> map = Map.of(
                    "encounterId", encounter.getEncounterId(),
                    "objectAId", encounter.getObjectAId(),
                    "objectBId", encounter.getObjectBId(),
                    "minSeparationKm", encounter.getMinSeparationKm(),
                    "timeToClosestApproachSeconds", encounter.getTimeToClosestApproachSeconds(),
                    "relativeVelocityKmPerSec", encounter.getRelativeVelocityKmPerSec(),
                    "riskScore", riskScore,
                    "status", encounter.getStatus().name(),
                    "detectedAtTick", encounter.getDetectedAtTick()
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
