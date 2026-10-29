
package com.orbitalnet.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class EncounterTracker {

    private final String runId;
    private final List<Encounter> encounters = new ArrayList<>();

    // Stores the most recent detection tick for each object pair.
    private final Map<String, Integer> lastDetectedTick = new HashMap<>();
    // Counts consecutive ticks in which each object pair is not detected.
    private final Map<String, Integer> missingTicks = new HashMap<>();

    // Stores the current ongoing encounter for each object pair.
    private final Map<String, Encounter> ongoingEncounters = new HashMap<>();
    private final Map<String, Encounter> pendingResolution = new HashMap<>();
    private int lastProcessedTick = -1;

    public EncounterTracker(String runId) {
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("Run ID cannot be blank.");
        }

        this.runId = runId;
    }

    public List<Encounter> processTick(
            int tick,
            List<EncounterDetectionService.DetectedEncounter> detections,
            List<SpaceObject> currentObjects) {

        if (tick < 0 || tick <= lastProcessedTick) {
            throw new IllegalArgumentException(
                    "Ticks must be non-negative and strictly increasing.");
        }

        if (detections == null) {
            throw new IllegalArgumentException("Detections cannot be null.");
        }

        if (currentObjects == null) {
            throw new IllegalArgumentException("Current objects cannot be null.");
        }

        Set<String> detectedPairs = new HashSet<>();
        List<Encounter> updatedThisTick = new ArrayList<>();

        for (EncounterDetectionService.DetectedEncounter detection : detections) {
            if (detection == null) {
                throw new IllegalArgumentException(
                        "Detection list cannot contain null.");
            }

            String pairKey = pairKey(
                    detection.getObjectAId(),
                    detection.getObjectBId());

            if (!detectedPairs.add(pairKey)) {
                throw new IllegalArgumentException(
                        "Duplicate detection for the same object pair in one tick.");
            }

            Integer previousTick = lastDetectedTick.get(pairKey);
            Encounter ongoing = ongoingEncounters.get(pairKey);

            boolean continuesPreviousEvent =
                    previousTick != null
                    && missingTicks.getOrDefault(pairKey, 0) < 2
                    && ongoing != null
                    && ongoing.getStatus() != Encounter.Status.RESOLVED;


            Encounter encounter;

            if (continuesPreviousEvent) {
                encounter = ongoing;

                encounter.updateMeasurements(
                        detection.getMinSeparationKm(),
                        detection.getTimeToClosestApproachSeconds(),
                        detection.getRelativeVelocityKmPerSec());
            } else {
                // A returning detection starts a new encounter if the
                // previous continuity window has already ended.
                Encounter previousPending = pendingResolution.remove(pairKey);

                if (previousPending != null
                        && hasNaturallyEnded(previousPending, currentObjects)) {
                    previousPending.resolve();
                }

                encounter = new Encounter(
                        UUID.randomUUID().toString(),
                        runId,
                        detection.getObjectAId(),
                        detection.getObjectBId(),
                        detection.getMinSeparationKm(),
                        detection.getTimeToClosestApproachSeconds(),
                        detection.getRelativeVelocityKmPerSec(),
                        tick);

                encounters.add(encounter);
                ongoingEncounters.put(pairKey, encounter);
            }


            lastDetectedTick.put(pairKey, tick);
            missingTicks.put(pairKey, 0);
            updatedThisTick.add(encounter);
        }



        // Count consecutive missing ticks for ongoing encounters.
        for (String pairKey : new ArrayList<>(ongoingEncounters.keySet())) {
            if (!detectedPairs.contains(pairKey)) {
                int count = missingTicks.getOrDefault(pairKey, 0) + 1;
                missingTicks.put(pairKey, count);

                if (count >= 2) {
                    Encounter encounter = ongoingEncounters.remove(pairKey);

                    if (encounter != null
                            && encounter.getStatus() != Encounter.Status.RESOLVED) {

                        pendingResolution.put(pairKey, encounter);
                    }
                }
            }
        }

        // Recheck encounters awaiting confirmed resolution.
        for (String pairKey : new ArrayList<>(pendingResolution.keySet())) {
            if (!detectedPairs.contains(pairKey)) {
                Encounter encounter = pendingResolution.get(pairKey);

                if (hasNaturallyEnded(encounter, currentObjects)) {
                    encounter.resolve();
                    pendingResolution.remove(pairKey);
                    missingTicks.remove(pairKey);
                }
            }
        }


        lastProcessedTick = tick;

        return Collections.unmodifiableList(updatedThisTick);
    }

    public List<Encounter> getEncounters() {
        return Collections.unmodifiableList(encounters);
    }

    private String pairKey(String objectAId, String objectBId) {
        if (objectAId == null || objectAId.isBlank()
                || objectBId == null || objectBId.isBlank()) {
            throw new IllegalArgumentException("Object IDs cannot be blank.");
        }

        if (objectAId.equals(objectBId)) {
            throw new IllegalArgumentException(
                    "An encounter requires two different objects.");
        }

        if (objectAId.compareTo(objectBId) < 0) {
            return objectAId + "|" + objectBId;
        }

        return objectBId + "|" + objectAId;
    }

    private boolean hasNaturallyEnded(
        Encounter encounter,
        List<SpaceObject> currentObjects) {

        SpaceObject objectA = null;
        SpaceObject objectB = null;

        for (SpaceObject object : currentObjects) {
            if (object.getId().equals(encounter.getObjectAId())) {
                objectA = object;
            }

            if (object.getId().equals(encounter.getObjectBId())) {
                objectB = object;
            }
        }

        // We cannot confirm a natural ending if either object is missing.
        if (objectA == null || objectB == null) {
            return false;
        }

        // Inactive or destroyed objects do not establish a natural ending.
        if (!objectA.isActive() || !objectB.isActive()) {
            return false;
        }

        Vector3D relativePosition =
                objectB.getPosition().subtract(objectA.getPosition());

        Vector3D relativeVelocity =
                objectB.getVelocity().subtract(objectA.getVelocity());

        double currentSeparationKm =
                objectA.getPosition().distanceTo(objectB.getPosition());

        boolean outsideDetectionRange =
                currentSeparationKm > EncounterDetectionService.DETECTION_DISTANCE_KM;

        boolean movingApart =
                relativePosition.dot(relativeVelocity) > 0;

        return outsideDetectionRange && movingApart;
    }
}
