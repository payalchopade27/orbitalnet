package backend;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/simulations")
@CrossOrigin(origins = "http://localhost:5173")
public class SimulationController {

    private final SimulationRunService simulationRunService;

    public SimulationController(
            SimulationRunService simulationRunService) {

        this.simulationRunService = simulationRunService;
    }

    @PostMapping("/{scenarioId}/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> runSimulation(
            @PathVariable String scenarioId) {

        Map<String, Object> run =
                simulationRunService.createRun(scenarioId);

        if (run == null) {

            return Map.of(
                    "status", "FAILED",
                    "message", "Scenario not found",
                    "scenarioId", scenarioId
            );
        }

        return Map.of(
                "status", "PENDING",
                "message", "Simulation request accepted",
                "runId", run.get("runId"),
                "scenarioId", scenarioId
        );
    }

    @GetMapping("/{runId}/state")
    public Map<String, Object> getSimulationState(
            @PathVariable String runId) {

        Map<String, Object> run =
                simulationRunService.getRun(runId);

        if (run == null) {

            return Map.of(
                    "status", "NOT_FOUND",
                    "message", "Simulation run not found",
                    "runId", runId
            );
        }

        return run;
    }

    @PostMapping("/{runId}/advance")
    public Map<String, Object> advanceSimulation(
            @PathVariable String runId) {

        Map<String, Object> run =
                simulationRunService.advanceRun(runId);

        if (run == null) {

            return Map.of(
                    "status", "NOT_FOUND",
                    "message", "Simulation run not found",
                    "runId", runId
            );
        }

        return run;
    }
}