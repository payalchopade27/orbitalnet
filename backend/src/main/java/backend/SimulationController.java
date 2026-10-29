package backend;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Scenario not found. Generate a scenario first."
            );
        }

        return Map.of(
                "message", "Simulation request accepted",
                "scenarioId", run.get("scenarioId"),
                "runId", run.get("runId"),
                "status", run.get("status")
        );
    }

    @GetMapping("/{runId}/state")
    public Map<String, Object> getSimulationState(
            @PathVariable String runId) {

        Map<String, Object> run =
                simulationRunService.getRun(runId);

        if (run == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Simulation run not found"
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
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Simulation run not found"
            );
        }

        return run;
    }
}