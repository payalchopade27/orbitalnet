package backend;

import backend.model.Scenario;
import backend.repository.ScenarioRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;

    public ScenarioService(ScenarioRepository scenarioRepository) {
        this.scenarioRepository = scenarioRepository;
    }

    public void saveScenario(ScenarioResponse scenario) {

        try {

            String generationConfig =
                    "{\"satellites\":" + scenario.getSatellites()
                    + ",\"debris\":" + scenario.getDebris()
                    + ",\"duration\":" + scenario.getDuration() + "}";

            Scenario dbScenario = new Scenario(
                    scenario.getScenarioId(),
                    "Scenario " + scenario.getScenarioId(),
                    scenario.getScenarioId().hashCode(),
                    generationConfig,
                    scenario.getStatus(),
                    null
            );

            scenarioRepository.save(dbScenario);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to save scenario",
                    e
            );
        }
    }

    public ScenarioResponse getScenario(String scenarioId) {

        try {

            Scenario scenario =
                    scenarioRepository.findById(scenarioId);

            if (scenario == null) {
                return null;
            }

            String config =
                    scenario.getGenerationConfig();

            int satellites =
                    extractInt(config, "satellites");

            int debris =
                    extractInt(config, "debris");

            int duration =
                    extractInt(config, "duration");

            return new ScenarioResponse(
                    scenario.getScenarioId(),
                    "Scenario loaded",
                    scenario.getStatus(),
                    satellites,
                    debris,
                    duration
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to get scenario",
                    e
            );
        }
    }

    public List<Scenario> getAllScenarios() {

        try {

            return scenarioRepository.findAll();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to get scenarios",
                    e
            );
        }
    }

    private int extractInt(
            String json,
            String key) {

        String search =
                "\"" + key + "\"";

        int keyPosition =
                json.indexOf(search);

        if (keyPosition == -1) {
            return 0;
        }

        int colonPosition =
                json.indexOf(
                        ":",
                        keyPosition
                );

        if (colonPosition == -1) {
            return 0;
        }

        int start =
                colonPosition + 1;

        while (start < json.length()
                && Character.isWhitespace(
                        json.charAt(start))) {

            start++;
        }

        int end = start;

        while (end < json.length()
                && Character.isDigit(
                        json.charAt(end))) {

            end++;
        }

        if (start == end) {
            return 0;
        }

        return Integer.parseInt(
                json.substring(start, end)
        );
    }
}