
package backend;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class ScenarioService {

    private final Map<String, ScenarioResponse> scenarios =
            new ConcurrentHashMap<>();

    public void saveScenario(ScenarioResponse scenario) {
        scenarios.put(scenario.getScenarioId(), scenario);
    }

    public ScenarioResponse getScenario(String scenarioId) {
        return scenarios.get(scenarioId);
    }
}

