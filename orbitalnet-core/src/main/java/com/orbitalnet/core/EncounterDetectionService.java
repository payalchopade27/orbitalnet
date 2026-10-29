
package com.orbitalnet.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EncounterDetectionService {

    public static final double DETECTION_DISTANCE_KM = 100.0;

    public List<DetectedEncounter> detect(List<SpaceObject> objects) {
        Objects.requireNonNull(objects, "Object list cannot be null.");

        List<DetectedEncounter> detected = new ArrayList<>();

        for (int i = 0; i < objects.size(); i++) {
            SpaceObject objectA = Objects.requireNonNull(
                    objects.get(i), "Object list cannot contain null.");

            if (!objectA.isActive()) {
                continue;
            }

            for (int j = i + 1; j < objects.size(); j++) {
                SpaceObject objectB = Objects.requireNonNull(
                        objects.get(j), "Object list cannot contain null.");

                if (!objectB.isActive()) {
                    continue;
                }

                ClosestApproach closestApproach = new ClosestApproach(
                        objectA.getPosition(),
                        objectA.getVelocity(),
                        objectB.getPosition(),
                        objectB.getVelocity()
                );

                if (closestApproach.getDistanceKm() <= DETECTION_DISTANCE_KM) {
                    Vector3D relativeVelocity = objectB.getVelocity()
                            .subtract(objectA.getVelocity());

                    double relativeSpeed = relativeVelocity.magnitude();

                    boolean collisionCondition =
                            closestApproach.getDistanceKm()
                                    <= objectA.getSize() + objectB.getSize();

                    detected.add(new DetectedEncounter(
                            objectA.getId(),
                            objectB.getId(),
                            closestApproach.getDistanceKm(),
                            closestApproach.getTimeSeconds(),
                            relativeSpeed,
                            collisionCondition
                    ));
                }
            }
        }

        return detected;
    }

    public static final class DetectedEncounter {

        private final String objectAId;
        private final String objectBId;
        private final double minSeparationKm;
        private final double timeToClosestApproachSeconds;
        private final double relativeVelocityKmPerSec;
        private final boolean collisionCondition;

        private DetectedEncounter(
                String objectAId,
                String objectBId,
                double minSeparationKm,
                double timeToClosestApproachSeconds,
                double relativeVelocityKmPerSec,
                boolean collisionCondition) {

            this.objectAId = objectAId;
            this.objectBId = objectBId;
            this.minSeparationKm = minSeparationKm;
            this.timeToClosestApproachSeconds = timeToClosestApproachSeconds;
            this.relativeVelocityKmPerSec = relativeVelocityKmPerSec;
            this.collisionCondition = collisionCondition;
        }

        public String getObjectAId() {
            return objectAId;
        }

        public String getObjectBId() {
            return objectBId;
        }

        public double getMinSeparationKm() {
            return minSeparationKm;
        }

        public double getTimeToClosestApproachSeconds() {
            return timeToClosestApproachSeconds;
        }

        public double getRelativeVelocityKmPerSec() {
            return relativeVelocityKmPerSec;
        }

        public boolean hasCollisionCondition() {
            return collisionCondition;
        }
    }
}
