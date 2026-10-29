
package com.orbitalnet.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ScenarioGenerator {

    public Scenario generate(
            String scenarioId,
            ScenarioGenerationConfig config,
            long seed) {

        if (config == null) {
            throw new IllegalArgumentException(
                    "Generation config cannot be null.");
        }

        Random random = new Random(seed);
        List<InitialObjectState> states = new ArrayList<>();

        for (int i = 1; i <= config.getSatelliteCount(); i++) {
            String id = "SAT-" + i;

            InitialObjectState state = new InitialObjectState(
                    id,
                    SpaceObject.ObjectType.SATELLITE,
                    generatePosition(random, config),
                    generateVelocity(random, config),
                    generateSize(random, config),
                    SpaceObject.Status.ACTIVE,
                    "Satellite-" + i,
                    null
            );

            states.add(state);
        }

        for (int i = 1; i <= config.getDebrisCount(); i++) {
            String id = "DEB-" + i;

            InitialObjectState state = new InitialObjectState(
                    id,
                    SpaceObject.ObjectType.DEBRIS,
                    generatePosition(random, config),
                    generateVelocity(random, config),
                    generateSize(random, config),
                    SpaceObject.Status.ACTIVE,
                    null,
                    null
            );

            states.add(state);
        }

        return new Scenario(
                scenarioId,
                config,
                seed,
                states,
                Scenario.Status.READY
        );
    }

    private Vector3D generatePosition(
            Random random, ScenarioGenerationConfig config) {

        return new Vector3D(
                randomInRange(
                        random,
                        config.getMinPositionKm(),
                        config.getMaxPositionKm()),
                randomInRange(
                        random,
                        config.getMinPositionKm(),
                        config.getMaxPositionKm()),
                randomInRange(
                        random,
                        config.getMinPositionKm(),
                        config.getMaxPositionKm())
        );
    }

    private Vector3D generateVelocity(
            Random random, ScenarioGenerationConfig config) {

        return new Vector3D(
                randomInRange(
                        random,
                        config.getMinVelocityKmPerSec(),
                        config.getMaxVelocityKmPerSec()),
                randomInRange(
                        random,
                        config.getMinVelocityKmPerSec(),
                        config.getMaxVelocityKmPerSec()),
                randomInRange(
                        random,
                        config.getMinVelocityKmPerSec(),
                        config.getMaxVelocityKmPerSec())
        );
    }

    private double generateSize(
            Random random, ScenarioGenerationConfig config) {

        return randomInRange(
                random,
                config.getMinSizeKm(),
                config.getMaxSizeKm());
    }

    private double randomInRange(
            Random random, double minimum, double maximum) {

        if (minimum == maximum) {
            return minimum;
        }

        return minimum + random.nextDouble() * (maximum - minimum);
    }


    public Scenario generateKnownThreatScenario(
            String scenarioId, long seed) {

        List<InitialObjectState> states = new ArrayList<>();

        InitialObjectState satellite = new InitialObjectState(
                "SAT-1",
                SpaceObject.ObjectType.SATELLITE,
                new Vector3D(-100, 0, 0),
                new Vector3D(1, 0, 0),
                1.0,
                SpaceObject.Status.ACTIVE,
                "Satellite-1",
                null
        );

        InitialObjectState debris = new InitialObjectState(
                "DEB-1",
                SpaceObject.ObjectType.DEBRIS,
                new Vector3D(100, 0, 0),
                new Vector3D(-1, 0, 0),
                1.0,
                SpaceObject.Status.ACTIVE,
                null,
                null
        );

        states.add(satellite);
        states.add(debris);

        ScenarioGenerationConfig config =
                new ScenarioGenerationConfig(
                        1, 1,
                        -100, 100,
                        -1, 1,
                        1.0, 1.0
                );

        return new Scenario(
                scenarioId,
                config,
                seed,
                states,
                Scenario.Status.READY
        );
    }

}
