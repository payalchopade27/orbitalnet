
package backend;

import backend.model.Scenario;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/scenarios")
@CrossOrigin(origins = "http://localhost:5173")
public class ScenarioController {

    private final ScenarioService scenarioService;

    public ScenarioController(ScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public ScenarioResponse generateScenario(
            @Valid @RequestBody ScenarioRequest request) {

        ScenarioResponse scenario = new ScenarioResponse(
                UUID.randomUUID().toString(),
                "Scenario generated successfully",
                "READY",
                request.getSatellites(),
                request.getDebris(),
                request.getDuration()
        );

        scenarioService.saveScenario(scenario);

        return scenario;
    }

    @GetMapping
    public List<Scenario> getAllScenarios() {
        return scenarioService.getAllScenarios();
    }

    @GetMapping("/{scenarioId}")
    public ScenarioResponse getScenario(
            @PathVariable String scenarioId) {

        ScenarioResponse scenario =
                scenarioService.getScenario(scenarioId);

        if (scenario == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Scenario not found"
            );
        }

        return scenario;
    }
}
