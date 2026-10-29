
package com.orbitalnet.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Scenario {

    public enum Status {
        READY,
        FAILED
    }

    private final String scenarioId;
    private final ScenarioGenerationConfig generationConfig;
    private final long seed;
    private final List<InitialObjectState> initialObjectStates;
    private final Status status;

    public Scenario(
            String scenarioId,
            ScenarioGenerationConfig generationConfig,
            long seed,
            List<InitialObjectState> initialObjectStates,
            Status status) {

        if (scenarioId == null || scenarioId.isBlank()) {
            throw new IllegalArgumentException(
                    "Scenario ID cannot be blank.");
        }

        this.scenarioId = scenarioId;
        this.generationConfig = Objects.requireNonNull(
                generationConfig, "Generation config cannot be null.");

        Objects.requireNonNull(
                initialObjectStates, "Initial object states cannot be null.");

        for (InitialObjectState state : initialObjectStates) {
            Objects.requireNonNull(state, "Initial object state cannot be null.");
        }

        this.initialObjectStates = Collections.unmodifiableList(
                new ArrayList<>(initialObjectStates));

        this.seed = seed;
        this.status = Objects.requireNonNull(
                status, "Scenario status cannot be null.");
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public ScenarioGenerationConfig getGenerationConfig() {
        return generationConfig;
    }

    public long getSeed() {
        return seed;
    }

    public List<InitialObjectState> getInitialObjectStates() {
        return initialObjectStates;
    }

    public Status getStatus() {
        return status;
    }
}
