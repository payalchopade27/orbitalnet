package backend;

import backend.model.Encounter;
import backend.model.FinalObjectState;
import backend.model.RiskAssessment;
import backend.model.SpaceObject;
import backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SimulationAdvanceIntegrationTest {

    @Autowired
    private SimulationRunService simulationRunService;

    @Autowired
    private SpaceObjectRepository spaceObjectRepository;

    @Autowired
    private EncounterRepository encounterRepository;

    @Autowired
    private RiskAssessmentRepository riskAssessmentRepository;

    @Autowired
    private FinalObjectStateRepository finalObjectStateRepository;

    @Autowired
    private EncounterController encounterController;

    @Test
    void testSimulationAdvanceAndManeuverLifecycle() throws Exception {
        String scenarioId = "56111e50-6fd9-4912-99b5-0c41e78a671e";

        System.out.println(">>> STEP 1: Creating a fresh, isolated test run for scenario " + scenarioId);
        // Protect existing run 0bce96c9-bae2-4361-9d80-9b3e8536b8b3 by creating a separate dynamic run
        Map<String, Object> createdRun = simulationRunService.createRun(scenarioId);
        assertNotNull(createdRun, "createRun should return created run map");
        String runId = (String) createdRun.get("runId");
        assertNotNull(runId, "Created run must have a valid runId");
        assertNotEquals("0bce96c9-bae2-4361-9d80-9b3e8536b8b3", runId, "Must not touch the protected existing run");
        System.out.println("Created test run: " + runId);

        System.out.println(">>> STEP 2: Verifying space objects are persisted for scenario");
        List<SpaceObject> objects = spaceObjectRepository.findByScenarioId(scenarioId);
        assertNotNull(objects);
        assertEquals(18, objects.size(), "Scenario should have all 18 objects persisted");
        Map<String, SpaceObject> objectsById = objects.stream()
                .collect(Collectors.toMap(SpaceObject::getObjectId, Function.identity()));
        System.out.println("Space objects verified in space_object: " + objects.size());

        System.out.println(">>> STEP 3: Advancing simulation test run " + runId);
        Map<String, Object> advanceResult = simulationRunService.advanceRun(runId);
        assertNotNull(advanceResult, "advanceRun should return state map");
        assertEquals(runId, advanceResult.get("runId"));
        assertEquals("RUNNING", advanceResult.get("status"));
        assertEquals(1, advanceResult.get("currentTick"));
        assertEquals(5.0, ((Number) advanceResult.get("elapsedTimeSeconds")).doubleValue(), 0.001);

        List<?> encounters = (List<?>) advanceResult.get("encounters");
        assertNotNull(encounters);
        assertFalse(encounters.isEmpty(), "At least one encounter should be detected at tick 1");
        System.out.println("Encounters returned in advance response: " + encounters.size());

        System.out.println(">>> STEP 4: Verifying encounter and risk assessment persistence");
        List<Encounter> savedEncounters = encounterRepository.findByRunId(runId);
        assertFalse(savedEncounters.isEmpty(), "Encounters should be persisted in database");
        String encounterId = savedEncounters.get(0).getEncounterId();
        String objA = savedEncounters.get(0).getObjectAId();
        String objB = savedEncounters.get(0).getObjectBId();
        assertNotNull(objA);
        assertNotNull(objB);

        // Verify encounter objects against persisted scenario objects without assuming pair ordering
        assertTrue(objectsById.containsKey(objA), "Encounter object A (" + objA + ") must exist in persisted space_objects");
        assertTrue(objectsById.containsKey(objB), "Encounter object B (" + objB + ") must exist in persisted space_objects");

        SpaceObject soA = objectsById.get(objA);
        SpaceObject soB = objectsById.get(objB);
        assertNotEquals(soA.getObjectType(), soB.getObjectType(), "Encounter must be between distinct types (SATELLITE vs DEBRIS)");

        SpaceObject satObj = "SATELLITE".equalsIgnoreCase(soA.getObjectType()) ? soA : soB;
        SpaceObject debObj = "DEBRIS".equalsIgnoreCase(soA.getObjectType()) ? soA : soB;
        assertNotNull(satObj, "Encounter must include a satellite");
        assertNotNull(debObj, "Encounter must include a debris object");
        System.out.println("Verified encounter between satellite: " + satObj.getObjectId() + " and debris: " + debObj.getObjectId());

        RiskAssessment risk = riskAssessmentRepository.findByEncounterId(encounterId);
        assertNotNull(risk, "Risk assessment should be persisted for encounter");
        assertTrue(risk.getTotalScore() >= 0 && risk.getTotalScore() <= 100);
        System.out.println("Persisted encounter " + encounterId + " with risk score: " + risk.getTotalScore());

        System.out.println(">>> STEP 5: Verifying state retrieval (GET simulation state)");
        Map<String, Object> state = simulationRunService.getRun(runId);
        assertNotNull(state);
        assertEquals("RUNNING", state.get("status"));
        assertEquals(1, state.get("currentTick"));
        List<?> stateEncounters = (List<?>) state.get("encounters");
        assertNotNull(stateEncounters);
        assertFalse(stateEncounters.isEmpty(), "State endpoint should return encounters");

        System.out.println(">>> STEP 6: Testing maneuver candidate generation");
        ResponseEntity<?> maneuverResponse = encounterController.createManeuvers(encounterId);
        assertEquals(200, maneuverResponse.getStatusCode().value());
        assertNotNull(maneuverResponse.getBody());
        List<?> candidates = (List<?>) maneuverResponse.getBody();
        assertFalse(candidates.isEmpty(), "At least one maneuver candidate should be generated");
        Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);
        String candidateId = (String) firstCandidate.get("maneuverCandidateId");
        assertNotNull(candidateId);
        assertEquals(satObj.getObjectId(), firstCandidate.get("targetObjectId"),
                "Maneuver candidate target object must match the encounter's satellite ID");
        System.out.println("Maneuver candidates generated successfully, first ID: " + candidateId);

        System.out.println(">>> STEP 7: Testing complete maneuver application flow");
        ResponseEntity<?> applyResponse = encounterController.applyManeuver(encounterId, candidateId);
        assertEquals(200, applyResponse.getStatusCode().value(), "applyManeuver should return HTTP 200");
        Map<?, ?> applyResult = (Map<?, ?>) applyResponse.getBody();
        assertNotNull(applyResult);

        // Verify full response contract
        assertEquals("Maneuver applied successfully", applyResult.get("message"));
        assertEquals(runId, applyResult.get("baselineRunId"), "Baseline run ID must match the advanced test run");
        String maneuverRunId = (String) applyResult.get("maneuverRunId");
        assertNotNull(maneuverRunId, "Maneuver run ID should be returned");
        assertEquals(candidateId, applyResult.get("maneuverCandidateId"), "Candidate ID must match applied candidate");
        assertEquals(satObj.getObjectId(), applyResult.get("targetObjectId"), "Target object must match the satellite ID");
        assertNotNull(applyResult.get("deltaV"), "Delta-V vector map must be present");
        assertEquals("COMPLETED", applyResult.get("status"), "Maneuver run status must be COMPLETED");
        System.out.println("Maneuver applied successfully with maneuverRunId: " + maneuverRunId);

        // Verify final object states via Spring injected FinalObjectStateRepository
        List<FinalObjectState> finalStates = finalObjectStateRepository.findByRunId(maneuverRunId);
        assertNotNull(finalStates);
        assertEquals(18, finalStates.size(), "All 18 objects should have final states recorded in final_object_state");
        for (FinalObjectState fs : finalStates) {
            assertEquals(maneuverRunId, fs.getRunId(), "Every final state record must belong to the maneuver run ID");
            assertNotNull(fs.getObjectId(), "Object ID must not be null in final state");
        }
        System.out.println("Final object states verified for maneuver run: " + finalStates.size());

        System.out.println(">>> ALL INTEGRATION ASSERTIONS PASSED FOR TEST RUN " + runId);
    }
}
