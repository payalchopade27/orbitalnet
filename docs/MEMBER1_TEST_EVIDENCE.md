# OrbitalNet — Member 1 Core Engine Test Evidence & Traceability Report

**Module:** `orbitalnet-core` (`com.orbitalnet.core`)  
**Role:** Member 1 (Core Engine & FODS)  
**Branch:** `member1-core`  
**Target Consumer:** Member 3 (Software Engineering Test Plan & Quality Documentation)  
**Document Purpose:** This document provides the authoritative test inventory, execution evidence, and edge-case verification for the 215 automated JUnit 5 tests implemented by Member 1. It is designed to be directly incorporated into Member 3's Software Engineering Test Plan, traceability matrices, and final project report.

---

## 1. Test Suite Execution & Verification Metadata

The automated test suite for `orbitalnet-core` was verified using Maven Surefire with bounded memory configuration.

| Parameter | Recorded Value / Evidence Source |
|---|---|
| **Command Line** | `mvn -f .\orbitalnet-core\pom.xml test -DargLine="-Xmx256m"` |
| **Java Environment** | Eclipse JustJ OpenJDK Hotspot JRE 21.0.11 (`21.0.11+9-LTS`) |
| **Build Tool** | Apache Maven 3.10.0 |
| **Surefire Provider** | `org.apache.maven.surefire.junitplatform.JUnitPlatformProvider` (Surefire 3.5.2) |
| **Test Framework** | JUnit Jupiter 5.11.4 |
| **Total Test Classes** | **27 test classes** (verified from source and XML reports) |
| **Total Tests Run** | **215 tests** |
| **Failures** | **0** |
| **Errors** | **0** |
| **Skipped** | **0** |
| **Build Status** | **BUILD SUCCESS** |
| **Total Maven Clock Time** | **10.340 s** (Task 1359 execution log) |
| **Cumulative Test Runtime** | **6.485 s** (sum of individual Surefire test suite XML runtimes) |
| **Report Storage** | `orbitalnet-core/target/surefire-reports/` (27 `.txt` and 27 `TEST-*.xml` files) |

*Reconciliation Note:* The count of 27 test classes and 215 test methods is exactly reconciled between the Java test source code in `src/test/java/com/orbitalnet/core/` and the Maven Surefire XML execution reports in `target/surefire-reports/`.

---

## 2. Subsystem Summary Table

The 215 tests are categorized across the nine explicit functional subsystems defined in `PROJECT_CONTEXT.md` §18 ("Member 1 Tests") and the *Member 1 Core Engine Execution Checklist*:

| # | Subsystem Category | Test Classes Included | Test Classes Count | `@Test` Count | Pass / Fail |
|:---:|---|---|:---:|:---:|:---:|
| **1** | **Domain Models & Vector Operations** | `Vector3DTest`, `SpaceObjectTest`, `SatelliteTest`, `DebrisTest`, `OrbitTest`, `InitialObjectStateTest`, `ScenarioTest`, `ScenarioGenerationConfigTest`, `ScenarioGeneratorTest`, `KnownThreatScenarioTest` | 10 | 49 | 49 / 0 |
| **2** | **Simulation Clock & Motion Propagation** | `SimulationRunTest` | 1 | 17 | 17 / 0 |
| **3** | **Closest Approach Geometry** | `ClosestApproachTest` | 1 | 5 | 5 / 0 |
| **4** | **Encounter Detection & Tracking Lifecycle** | `EncounterTest`, `EncounterDetectionServiceTest`, `EncounterTrackerTest` | 3 | 24 | 24 / 0 |
| **5** | **Risk Scoring Engine** | `RiskScoreTest` | 1 | 11 | 11 / 0 |
| **6** | **Maneuver Planning & Retrospective Replay** | `ManeuverPlannerTest`, `ManeuverReplayServiceTest` | 2 | 21 | 21 / 0 |
| **7** | **Custom FODS Data Structures** | `CustomHashTableTest`, `SpatialGridIndexTest`, `CustomMaxHeapTest`, `CustomStackTest`, `CustomQueueTest`, `CustomSorterTest`, `OrbitalGraphTest` | 7 | 73 | 73 / 0 |
| **8** | **Performance Benchmarking** | `SpatialGridBenchmarkTest` | 1 | 4 | 4 / 0 |
| **9** | **Engine Facade, Determinism & Boundaries** | `OrbitalEngineTest` | 1 | 11 | 11 / 0 |
| **TOTAL** | **Full Core Engine Test Suite** | **All 27 Test Classes** | **27** | **215** | **215 / 0** |

---

## 3. Acceptance-Critical & Edge-Case Test Evidence Table

In accordance with Section 12 of the *OrbitalNet Team Execution Guide* (`Test ID | Test class and method | Input / scenario | Expected result | Actual result | Pass/fail`), the table below provides detailed evidence for all required edge cases, mathematical invariants, lifecycle boundaries, and workflow gates.

*Note on Identifiers:* IDs prefixed with `CORE-TEST-` are stable evidence-document references for Member 3's test plan and do not invent unverified SRS IDs.

| Test ID | Test Class & Method | Subsystem / Feature | Input / Scenario | Expected Result | Actual Result | Status |
|---|---|---|---|---|---|:---:|
| `CORE-TEST-001` | `ClosestApproachTest.shouldFindClosestApproachWhenObjectsMoveTowardsEachOther` | Closest Approach | Head-on approach: $r_A=(-100,0,0), v_A=(1,0,0)$, $r_B=(100,0,0), v_B=(-1,0,0)$. | TCA $t^* = 100.0\text{ s}$, minimum separation $= 0.0\text{ km}$, relative velocity $= 2.0\text{ km/s}$. | Exact match within $10^{-6}\text{ km}$; formula $t^* = -(r \cdot v)/\|v\|^2$ verified. | **PASS** |
| `CORE-TEST-002` | `ClosestApproachTest.shouldUseTimeZeroWhenObjectsAreMovingApart` | Closest Approach (Moving Apart) | Objects moving apart: $r_A=(0,0,0), v_A=(-1,0,0)$, $r_B=(50,0,0), v_B=(1,0,0)$. Formula yields $t^* < 0$. | Clamped to $t^* = 0.0\text{ s}$; minimum separation equals initial distance $50.0\text{ km}$. | $t^* = 0.0\text{ s}$, min separation $= 50.0\text{ km}$ verified. | **PASS** |
| `CORE-TEST-003` | `ClosestApproachTest.shouldHandleObjectsWithIdenticalVelocities` | Edge Case: Zero Relative Velocity | Parallel paths: $r_A=(0,0,0), r_B=(0,5,0)$, $v_A=v_B=(1,0,0)$. Relative velocity $\|v\|=0$. | Zero-velocity guard triggers: $t^* = 0.0\text{ s}$, separation $= 5.0\text{ km}$ for all time. | $t^* = 0.0\text{ s}$, separation $= 5.0\text{ km}$ verified without division by zero. | **PASS** |
| `CORE-TEST-004` | `ClosestApproachTest.shouldHandleObjectsWithIdenticalInitialPositionsAndZeroRelativeVelocity` | Edge Case: Identical Positions | $r_A = r_B = (100, 200, 300)$, $v_A = v_B = (1, 0, 0)$. Identical coordinates at $t=0$. | $t^* = 0.0\text{ s}$, minimum separation $= 0.0\text{ km}$. | $t^* = 0.0\text{ s}$, separation $= 0.0\text{ km}$ verified. | **PASS** |
| `CORE-TEST-005` | `ClosestApproachTest.shouldClampClosestApproachTimeToHorizonWhenBeyond3600Seconds` | Horizon Clamping | Slow approach where unconstrained $t^* > 3600\text{ s}$ ($5000\text{ s}$). | $t^*$ clamped to simulation horizon boundary $3600.0\text{ s}$; separation computed at $t=3600\text{ s}$. | Clamped to $3600.0\text{ s}$ verified. | **PASS** |
| `CORE-TEST-006` | `Vector3DTest.shouldHandleNegativeCoordinatesInAllOperations` | Vector Invariants | Negative coordinates: $v_1=(-10, -20, 30)$, $v_2=(5, -10, -15)$. Operations: add, subtract, scale, dot, magnitude, distance. | Exact Cartesian arithmetic: $v_1+v_2=(-5,-30,15)$, $v_1-v_2=(-15,-10,45)$, scale($-2$)$=(20,40,-60)$, dot $=-300.0$. | Exact coordinate match; zero precision drift. | **PASS** |
| `CORE-TEST-007` | `Vector3DTest.distanceBetweenIdenticalVectorsShouldBeZero` | Vector Invariants | Distance between identical vector instances with arbitrary coordinates $(-15.5, 42.1, -99.9)$. | Distance is exactly $0.0$. | `distanceTo` returns `0.0` within `0.000001` delta. | **PASS** |
| `CORE-TEST-008` | `Vector3DTest.operationsShouldNotMutateInputVectors` | Immutability | Call `add`, `subtract`, `scale` on $v_1=(1,2,3)$. | Original $v_1$ remains strictly $(1,2,3)$ without state mutation. | $v_1$ coordinates unchanged post-operations. | **PASS** |
| `CORE-TEST-009` | `EncounterDetectionServiceTest.shouldReturnNoResultsForSingleObjectInput` | Edge Case: Exactly One Object | List containing single valid active satellite passed to `detect()`. | Returns empty encounter list (encounters require distinct object pairs). | `encounters.isEmpty()` evaluates to `true`. | **PASS** |
| `CORE-TEST-010` | `EncounterDetectionServiceTest.shouldDetectObjectsWhoseClosestApproachIsWithin100Km` | Detection Threshold | Two objects with predicted closest approach of $95.0\text{ km} \le 100.0\text{ km}$. | Qualifies as encounter; detected encounter record produced. | Encounter list contains exactly 1 detected pair. | **PASS** |
| `CORE-TEST-011` | `EncounterDetectionServiceTest.shouldNotDetectObjectsBeyond100Km` | Detection Boundary | Two objects with predicted closest approach of $105.0\text{ km} > 100.0\text{ km}$. | Excluded from detection; returns empty list. | `encounters.isEmpty()` evaluates to `true`. | **PASS** |
| `CORE-TEST-012` | `EncounterDetectionServiceTest.shouldIgnoreInactiveObjects` | State Invariant | Inactive object placed in close proximity ($5.0\text{ km}$) to active object. | Inactive object excluded from collision checking. | `encounters.isEmpty()` evaluates to `true`. | **PASS** |
| `CORE-TEST-013` | `EncounterTest.shouldRejectSameObjectTwice` | Edge Case: Duplicate IDs | Construct encounter with `objectAId = "SAT-1"` and `objectBId = "SAT-1"`. | Throws `IllegalArgumentException("An encounter requires two different objects.")`. | Exception thrown and verified. | **PASS** |
| `CORE-TEST-014` | `EncounterTest.shouldStoreObjectIdsInCanonicalOrder` | Canonical Pair Key | Construct encounter with `objectA = "SAT-B"`, `objectB = "SAT-A"`. | Stored object IDs canonicalized lexicographically (`objectAId = "SAT-A"`, `objectBId = "SAT-B"`). | `getObjectAId() == "SAT-A"`, `getObjectBId() == "SAT-B"`. | **PASS** |
| `CORE-TEST-015` | `EncounterTrackerTest.detectionAfterOneMissedTickContinuesSameEncounter` | Continuity Gate | Pair detected at tick 0, absent at tick 1, re-detected at tick 2. | Single missed tick tolerance: maintains continuity of original encounter ID. | Encounter ID remains identical across ticks. | **PASS** |
| `CORE-TEST-016` | `EncounterTrackerTest.detectionAfterTwoConsecutiveMissedTicksCreatesNewEncounter` | Resolution Boundary | Pair detected at tick 0, absent at ticks 1 and 2, re-detected at tick 3. | Two missed ticks terminates event; re-detection generates a new encounter ID. | New distinct encounter ID generated; old encounter marked `RESOLVED`. | **PASS** |
| `CORE-TEST-017` | `EncounterTrackerTest.resolvedEncounterIsRetainedAndNotReopened` | Terminal State | Encounter reaches `RESOLVED` status after separation. | Retained in tracker list; never reopened or mutated. | Status remains `RESOLVED`; subsequent calls cannot alter it. | **PASS** |
| `CORE-TEST-018` | `RiskScoreTest.totalScoreMatchesHandCalculatedExamplesAndRounding` | Risk Engine Weights | Separation $10\text{ km}$ ($F_s=0.9$), relative velocity $3\text{ km/s}$ ($F_v=0.2$), TCA $360\text{ s}$ ($F_t=0.9$). Formula: $100 \times (0.50 \times 0.9 + 0.30 \times 0.2 + 0.20 \times 0.9)$. | Total score $= 69$ (High Risk); label maps to `HIGH`. | Total score $= 69$, label `RiskLabel.HIGH` verified. | **PASS** |
| `CORE-TEST-019` | `CustomMaxHeapTest.ranksByDescendingRiskScore` | FODS Heap Ordering | Encounters with risk scores 45, 92, 12, 78, 63 inserted into `CustomMaxHeap`. | Polling extracts in strict descending order: 92, 78, 63, 45, 12. | Array order `[92, 78, 63, 45, 12]` verified without standard library. | **PASS** |
| `CORE-TEST-020` | `CustomMaxHeapTest.deterministicTieBreakingUsesAscendingEncounterId` | Determinism Gate | Encounters with identical risk score 80 and IDs `"ENC-003"`, `"ENC-001"`, `"ENC-002"`. | Polling resolves ties deterministically using ascending encounter ID: `"ENC-001"`, `"ENC-002"`, `"ENC-003"`. | Extraction order strictly alphabetical on tie. | **PASS** |
| `CORE-TEST-021` | `ManeuverPlannerTest.plansCandidatesDeterministicallyWithoutMutatingBaselineObjects` | Maneuver Invariants | Plan avoidance maneuvers for target satellite and approaching debris. | Generates exactly 3 candidates (along-track, cross-track, radial) with orthogonal delta-v vectors and predicted separations $>100\text{ km}$; baseline objects unmutated. | 3 orthogonal candidates generated; target/debris state unmutated. | **PASS** |
| `CORE-TEST-022` | `OrbitalEngineTest.executesNoThreatScenarioProducingZeroEncountersAndEmptyRankings` | Edge Case: No-Threat Behavior | Scenario with objects placed at $(-500, 0, 0)$ and $(500, 0, 0)$ moving in parallel at $(0, 1, 0)$ (separation $1000\text{ km}$ throughout 720 ticks). | Full simulation baseline execution completes with 0 detected encounters, 0 ranked threats, and 0 graph edges. | `encounters.isEmpty()`, `rankedThreats.isEmpty()`, `getEdgeCount() == 0`. | **PASS** |
| `CORE-TEST-023` | `OrbitalEngineTest.executesBaselineRunDeterministicallyAcrossRepeatedInvocations` | Determinism Gate | Execute baseline simulation twice using identical scenario configuration and seed `42L`. | Run 1 and Run 2 produce identical object trajectories, encounter counts, IDs, metrics, risk scores, and threat ordering. | Bitwise equality across all baseline execution metrics. | **PASS** |
| `CORE-TEST-024` | `OrbitalEngineTest.plansManeuversAndReplaysViaFacade` | End-to-End Workflow | Execute authentic baseline run to completion; capture top threat (`RESOLVED`); construct `ManeuverReplayInput`; plan candidates at detection tick; execute replay. | Replay creates separate `SimulationRun`; applies delta-v at detection tick; displaces target according to oracle $\Delta r = \Delta v \times (3600 - 5 \times \text{tick})$; baseline remains terminal `RESOLVED`. | Displacement oracle matches within $10^{-6}\text{ km}$; baseline encounter untouched. | **PASS** |
| `CORE-TEST-025` | `OrbitalEngineTest.verifiesBaselineImmutabilityAcrossRetrospectiveReplay` | Baseline Immutability | Snapshot all baseline run properties, object vectors, encounter status (`RESOLVED`), TCA, and risk score before replay. | Post-replay assertion verifies 0 deviation across all snapshotted baseline attributes; result exposes separate replay outcome. | 100% snapshot match; zero state pollution. | **PASS** |

---

## 4. Complete Test Method Inventory by Subsystem

The following inventory accounts for all **215 test methods across all 27 test classes**, categorized into the 9 subsystems.

### 4.1 Domain Models & Vector Operations (10 classes, 49 tests)
* **`Vector3DTest` (11 tests):**
  1. `shouldAddVectors`: Vector vector addition $(x_1+x_2, y_1+y_2, z_1+z_2)$.
  2. `shouldSubtractVectors`: Vector subtraction $(x_1-x_2, y_1-y_2, z_1-z_2)$.
  3. `shouldScaleVector`: Scalar multiplication $(s \cdot x, s \cdot y, s \cdot z)$.
  4. `shouldCalculateDotProduct`: Dot product formula $(x_1 x_2 + y_1 y_2 + z_1 z_2)$.
  5. `shouldCalculateMagnitude`: Euclidean norm $\sqrt{x^2+y^2+z^2}$.
  6. `shouldCalculateDistance`: Euclidean distance $\|v_1 - v_2\|$.
  7. `vectorsWithSameCoordinatesShouldBeEqual`: Value object equality based on coordinates.
  8. `equalVectorsShouldHaveSameHashCode`: Equal objects produce identical hash codes.
  9. `shouldHandleNegativeCoordinatesInAllOperations`: Arithmetic correctness across all octants.
  10. `distanceBetweenIdenticalVectorsShouldBeZero`: Zero distance identity property.
  11. `operationsShouldNotMutateInputVectors`: Verification of immutable object contract.
* **`SpaceObjectTest` (6 tests):**
  1. `shouldStoreObjectProperties`: Constructor initialization and field exposure.
  2. `shouldReportWhetherObjectIsActive`: Status checking logic (`ACTIVE` vs `INACTIVE`/`DESTROYED`).
  3. `shouldAllowPositionVelocityAndStatusUpdates`: Direct position/velocity/status setters.
  4. `shouldMoveActiveObjectUsingVelocityAndTime`: Position advancement $r' = r + v \cdot \Delta t$.
  5. `shouldNotMoveInactiveObject`: Inactive objects remain stationary during tick advancement.
  6. `shouldNotMoveDestroyedObject`: Destroyed objects remain stationary during tick advancement.
* **`SatelliteTest` (2 tests):**
  1. `shouldStoreSatelliteProperties`: Name and orbit metadata binding.
  2. `shouldAllowMissingOrbitMetadata`: Optional orbit handling without NPE.
* **`DebrisTest` (1 test):**
  1. `shouldStoreDebrisProperties`: Debris-specific sizing, position, and velocity fields.
* **`OrbitTest` (1 test):**
  1. `shouldStoreOrbitMetadata`: Altitude ($km$), inclination ($deg$), and period ($s$).
* **`InitialObjectStateTest` (9 tests):**
  1. `shouldStoreSatelliteStateAndOrbit`: DTO validation for satellite template.
  2. `shouldStoreDebrisWithoutSatelliteMetadata`: DTO validation for debris template.
  3. `shouldRejectBlankId`: Validation rejecting empty/null object IDs.
  4. `shouldRejectInvalidSize`: Rejection of non-positive physical radius.
  5. `shouldRejectNullPosition`: Rejection of null coordinate vector.
  6. `shouldRejectSatelliteWithoutName`: Rejection of nameless satellites.
  7. `shouldRejectSatelliteMetadataForDebris`: Guard preventing debris with satellite metadata.
  8. `shouldReconstructSatellite`: Factory reconstruction into live `Satellite` instance.
  9. `shouldReconstructDebris`: Factory reconstruction into live `Debris` instance.
* **`ScenarioTest` (5 tests):**
  1. `shouldStoreScenarioDetails`: Scenario ID, status, and object state storage.
  2. `shouldProtectInitialStatesFromExternalListChanges`: Defensive copy on input.
  3. `shouldNotAllowModificationOfInitialStates`: Unmodifiable list exposure.
  4. `shouldRejectBlankScenarioId`: Blank scenario ID validation.
  5. `shouldSupportFailedStatus`: Support for `Scenario.Status.FAILED`.
* **`ScenarioGenerationConfigTest` (6 tests):**
  1. `shouldStoreGenerationSettings`: Validation of bounding boxes, counts, and velocities.
  2. `shouldAllowZeroObjectCounts`: Support for zero satellites or zero debris.
  3. `shouldRejectNegativeObjectCounts`: Guard against negative object counts.
  4. `shouldRejectInvalidRanges`: Guard against $\min > \max$ coordinate bounds.
  5. `shouldRejectNonFiniteBounds`: Guard against NaN/Infinity coordinate bounds.
  6. `shouldRejectNonPositiveObjectSize`: Guard against zero/negative object radii.
* **`ScenarioGeneratorTest` (6 tests):**
  1. `sameSeedAndConfigShouldProduceSameInitialStates`: Pseudo-random generation determinism.
  2. `shouldGenerateConfiguredCountsAndStableIds`: Verification of generated ID conventions.
  3. `shouldKeepGeneratedValuesWithinConfiguredBounds`: Range boundary adherence.
  4. `shouldReconstructFreshObjectsWithoutChangingScenario`: State immutability across spawns.
  5. `shouldMarkGeneratedScenarioReady`: Successful generation status `READY`.
  6. `shouldSupportAnEmptyScenario`: Zero-object generation scenario support.
* **`KnownThreatScenarioTest` (2 tests):**
  1. `shouldGeneratePredictableCloseApproachScenario`: Generates deterministic critical encounter.
  2. `shouldGenerateSameInitialStatesForSameSeed`: Seeded reproducibility of known threat.

### 4.2 Simulation Clock & Motion Propagation (1 class, 17 tests)
* **`SimulationRunTest` (17 tests):**
  1. `shouldCompleteRunThroughValidTransitions`: Lifecycle `PENDING` $\to$ `RUNNING` $\to$ `COMPLETED`.
  2. `shouldAllowFailingPendingRun`: Lifecycle transition `PENDING` $\to$ `FAILED`.
  3. `shouldAllowFailingRunningRun`: Lifecycle transition `RUNNING` $\to$ `FAILED`.
  4. `shouldRejectStartingRunMoreThanOnce`: Rejection of invalid `start()` on running run.
  5. `shouldRejectCompletingPendingRun`: Rejection of `complete()` on unstarted run.
  6. `shouldRejectFailingCompletedRun`: Completed run is terminal; cannot transition to `FAILED`.
  7. `shouldRejectFailingAlreadyFailedRun`: Failed run is terminal; cannot transition to `FAILED` again.
  8. `shouldAdvanceTickAndElapsedTime`: Tick counter increment ($+1$) and elapsed time ($+5.0\text{ s}$).
  9. `shouldStartAtTickZero`: Initial tick is 0, elapsed time is $0.0\text{ s}$.
  10. `shouldRejectTickAdvancementWhenNotRunning`: Rejection of `advanceTick()` on `PENDING`/`COMPLETED`.
  11. `shouldCreateWorkingObjectsFromScenario`: Object isolation from initial scenario definitions.
  12. `shouldKeepWorkingObjectsIndependentBetweenRuns`: Isolation between distinct run instances.
  13. `shouldMoveObjectsWhenAdvancingTick`: Object position advancement during tick progression.
  14. `shouldCompleteRunAtSimulationHorizon`: Automatic transition to `COMPLETED` at 720 ticks ($3600\text{ s}$).
  15. `shouldNotAdvanceBeyondSimulationHorizon`: Guard preventing advances past 720 ticks.
  16. `shouldNotMoveInactiveObjectDuringTick`: Inactive objects maintain coordinate stability.
  17. `shouldNotMoveDestroyedObjectDuringTick`: Destroyed objects maintain coordinate stability.

### 4.3 Closest Approach Geometry (1 class, 5 tests)
* **`ClosestApproachTest` (5 tests):**
  1. `shouldFindClosestApproachWhenObjectsMoveTowardsEachOther`: Analytical minimum distance formula.
  2. `shouldUseTimeZeroWhenObjectsAreMovingApart`: Past minimum distance clamped to $t=0$.
  3. `shouldHandleObjectsWithIdenticalVelocities`: Relative velocity zero handled safely.
  4. `shouldClampClosestApproachTimeToHorizonWhenBeyond3600Seconds`: Horizon clamp at $3600\text{ s}$.
  5. `shouldHandleObjectsWithIdenticalInitialPositionsAndZeroRelativeVelocity`: Zero distance at $t=0$.

### 4.4 Encounter Detection & Tracking Lifecycle (3 classes, 24 tests)
* **`EncounterTest` (8 tests):**
  1. `shouldStoreObjectIdsInCanonicalOrder`: Canonical lexicographical pair keying.
  2. `shouldStartWithDetectedStatus`: Default status is `DETECTED`.
  3. `shouldUpdateMeasurementsWithoutChangingDetectionTick`: `detectedAtTick` immutability.
  4. `shouldFollowValidLifecycle`: Valid transition `DETECTED` $\to$ `ASSESSED` $\to$ `MITIGATED` $\to$ `RESOLVED`.
  5. `shouldRejectInvalidLifecycleTransition`: Rejection of out-of-order transitions.
  6. `shouldRejectUpdatingResolvedEncounter`: Resolved encounters cannot accept new measurements.
  7. `shouldRejectSameObjectTwice`: Self-pair rejection.
  8. `shouldRejectNegativeSeparation`: Rejection of negative physical separation.
* **`EncounterDetectionServiceTest` (7 tests):**
  1. `shouldDetectObjectsWhoseClosestApproachIsWithin100Km`: True positive detection $\le 100\text{ km}$.
  2. `shouldDetectExactlyAt100Km`: Boundary threshold detection at $100.0\text{ km}$.
  3. `shouldNotDetectObjectsBeyond100Km`: True negative exclusion $> 100.0\text{ km}$.
  4. `shouldIgnoreInactiveObjects`: Inactive space objects excluded from pairwise checks.
  5. `shouldCheckEachPairOnlyOnce`: $O(N^2/2)$ single-evaluation pair guarantee.
  6. `shouldReturnNoResultsForEmptyInput`: Graceful handling of empty object collections.
  7. `shouldReturnNoResultsForSingleObjectInput`: Exactly one object produces 0 encounters.
* **`EncounterTrackerTest` (9 tests):**
  1. `consecutiveDetectionsUpdateSameEncounter`: Metric updates on continuous detection.
  2. `detectionAfterOneMissedTickContinuesSameEncounter`: Single-miss tolerance window.
  3. `detectionAfterTwoConsecutiveMissedTicksCreatesNewEncounter`: Multi-event split boundary.
  4. `reversedObjectOrderContinuesSameEncounter`: Pair order symmetry (`[A, B] == [B, A]`).
  5. `resolvedEncounterIsRetainedAndNotReopened`: Terminal retention of resolved encounters.
  6. `ticksMustIncreaseStrictly`: Monotonic tick progression requirement.
  7. `encounterResolvesAfterTwoMissedTicksWhenObjectsMoveApart`: Event auto-resolution logic.
  8. `encounterDoesNotResolveWhenObjectsAreStillMovingTowardEachOther`: In-progress tracking continuity.
  9. `shouldRejectDuplicateDetectionsForSamePairInSameTick`: Rejection of duplicate detection entries.

### 4.5 Risk Scoring Engine (1 class, 11 tests)
* **`RiskScoreTest` (11 tests):**
  1. `clampBehavesCorrectlyAcrossRanges`: Numerical clamping helper verification.
  2. `separationFactorCalculatesAndClampsCorrectly`: $F_s = \text{clamp}(1 - d/100)$.
  3. `velocityFactorCalculatesAndClampsCorrectly`: $F_v = \text{clamp}(v_{\text{rel}}/15)$.
  4. `timeFactorCalculatesAndClampsCorrectly`: $F_t = 1 - \text{clamp}(t_{\text{tca}}/3600)$.
  5. `totalScoreMatchesHandCalculatedExamplesAndRounding`: $50\% F_s + 30\% F_v + 20\% F_t$.
  6. `labelBoundariesMapCorrectly`: Low (0-24), Moderate (25-49), High (50-74), Critical (75-100).
  7. `deterministicExplanationFormatting`: Predictable plain-English explanation text.
  8. `calculateProducesCompleteValidRiskScore`: Value object integrity.
  9. `riskEngineAssessesDirectlyAndFromEncounter`: Facade evaluation method coverage.
  10. `encounterIntegrationAndReassessmentUpdatesLatestScore`: In-place encounter risk update.
  11. `riskScoreValidatesConstructorArguments`: Constructor parameter validation boundaries.

### 4.6 Maneuver Planning & Retrospective Replay (2 classes, 21 tests)
* **`ManeuverPlannerTest` (12 tests):**
  1. `maneuverCandidateModelValidatesInputsAndStoresProperties`: DTO validation.
  2. `plansCandidatesDeterministicallyWithoutMutatingBaselineObjects`: Non-mutating candidate generation.
  3. `knownThreatScenarioGeneratesFeasibleClearanceManeuver`: Feasible solution generation.
  4. `safetyFirstRankingWithDeterministicTieBreaks`: Clearance ranking with cost tie-breaking.
  5. `validationRejectsInvalidAssociationsAndDebrisTarget`: Debris maneuver rejection.
  6. `shouldHandleZeroVelocityTargetWithDeterministicCandidates`: Zero-velocity fallback kinematics.
  7. `shouldProduceOrthogonalVectorsAcrossReferenceAxisThreshold`: Orthogonal vector generation.
  8. `shouldRankLowerCostFirstWhenSeparationIsIdentical`: Secondary cost ranking.
  9. `shouldRankLexicographicallySmallerIdFirstWhenSeparationAndCostAreEqual`: Tertiary ID tie-breaking.
  10. `shouldReturnEmptyListWhenCustomCandidateListIsEmpty`: Empty candidate collection handling.
  11. `shouldPreserveEncounterMeasurementsAndStatusAcrossPlanning`: Domain state preservation.
  12. `shouldRejectInvalidDeltaVMagnitudes`: Non-positive impulse magnitude rejection.
* **`ManeuverReplayServiceTest` (9 tests):**
  1. `successfullyReplaysManeuverInSeparateRunAndPreservesBaseline`: Legacy adapter replay verification.
  2. `successfullyReplaysRetrospectiveManeuverUsingManeuverReplayInput`: Immutable retrospective replay.
  3. `replayProducesDeterministicResultsForIdenticalInputs`: Deterministic replay execution.
  4. `rejectsInvalidAssociationsAndStates`: Rejection of unassessed encounters and invalid states.
  5. `rejectsInvalidManeuverReplayInputAndCandidateCombinations`: Association validation.
  6. `appliesManeuverAtDetectedTickZeroBeforeFirstAdvance`: Tick 0 impulse timing verification.
  7. `failsManeuverRunAndPreservesBaselineWhenTargetIsInactiveAtManeuverTick`: Inactive target failure handling.
  8. `failsManeuverRunAndPreservesBaselineWhenTargetIsMissingAtManeuverTick`: Missing target failure handling.
  9. `populatesRecalculatedEncountersWithNewRunIdAndAssessedRiskScores`: Recalculated encounter output.

### 4.7 Custom FODS Data Structures (7 classes, 73 tests)
* **`CustomHashTableTest` (6 tests):**
  1. `shouldInsertAndRetrieveObject`: Insert and lookup by object ID.
  2. `shouldReturnNullForMissingId`: Non-existent ID returns null.
  3. `shouldHandleCollisions`: Separate chaining bucket traversal on hash collision.
  4. `shouldReplaceObjectWithSameId`: In-place value update for identical key.
  5. `shouldRejectNullObject`: Guard rejecting null object insertion.
  6. `shouldRejectNullOrBlankId`: Guard rejecting null or blank object ID lookup.
* **`SpatialGridIndexTest` (29 tests):**
  1. `mapsPositiveCoordinatesToCorrectCell`: Positive coordinate to cell index mapping via floor division.
  2. `handlesNegativeCoordinatesUsingFloor`: Mathematical floor handling for negative coordinates.
  3. `handlesExactCellBoundaries`: Boundary coordinate mapping integrity.
  4. `supportsCustomCellSize`: Constructor support for custom grid cell dimension.
  5. `rejectsInvalidCellSize`: Rejection of non-positive cell sizes.
  6. `rejectsNullPosition`: Rejection of null vector coordinate.
  7. `rejectsNonFinitePositionCoordinates`: Rejection of NaN or infinite coordinate components.
  8. `storesAndRetrievesObjectsInCell`: Multi-object cell bucket storage and retrieval.
  9. `doesNotAddSameObjectTwiceToOneCell`: Duplicate object prevention within single cell bucket.
  10. `ignoresInactiveObjects`: Inactive objects excluded from cell bucket insertion.
  11. `returnsEmptyListForUnknownCell`: Unpopulated cell returns empty list without exception.
  12. `indexesCellsAlongPredictedPath`: Swept-AABB broad-phase path expansion over simulation horizon.
  13. `excludesInactiveObjectsFromBuiltIndex`: Inactive object exclusion during full index build.
  14. `sendsVeryLargeSweptRegionToOverflowFallback`: Overflow fallback for high-velocity objects exceeding cell threshold.
  15. `emptyInputAndSingleActiveObjectProduceNoPairs`: Edge cases: 0 objects and 1 object produce zero candidate pairs.
  16. `twoObjectsSharingMultipleCellsProduceExactlyOnePair`: Unordered pair deduplication across multiple shared cells.
  17. `candidatePairGenerationExcludesInactiveObjects`: Candidate pair filtering excludes inactive objects.
  18. `pairsOverflowObjectsWithOtherActiveIndexedObjectsWithoutDuplicatesOrSelfPairs`: Complete cross-pairing of overflow objects with indexed active objects.
  19. `distinctPairsWithIdsContainingSeparatorsDoNotCollapse`: Delimiter-safe pair identity preventing collision on custom IDs.
  20. `rebuildingIndexClearsPreviousObjectsAndPairs`: Full index reset and cache clearing upon rebuild.
  21. `candidatePairCanonicalizesOrderAndEnforcesEqualityContract`: Value object equals/hashCode and canonical ordering for `CandidatePair`.
  22. `validateAgainstNaiveReferenceNegativeCoordinates`: Parity with naive $O(N^2)$ reference on negative coordinates.
  23. `validateAgainstNaiveReferenceStationaryObjects`: Parity with naive reference on stationary objects.
  24. `validateAgainstNaiveReferenceApproachingAndRecedingObjects`: Parity with naive reference on dynamic encounters.
  25. `validateAgainstNaiveReferenceCellBoundaries`: Parity with naive reference at exact cell boundary crossings.
  26. `validateAgainstNaiveReferenceInactiveObjectsExcluded`: Parity with naive reference on inactive object exclusion.
  27. `validateAgainstNaiveReferenceFastMovingOverflowObjects`: Parity with naive reference on high-velocity overflow objects.
  28. `validateAgainstNaiveReferenceKnownThreatScenario`: 100% true-positive recall on known-threat scenario.
  29. `validateAgainstNaiveReferenceRandomSeededScenarios`: Statistical parity and 100% recall over random seeded scenario batches.
* **`CustomMaxHeapTest` (13 tests):**
  1. `emptyHeapReportsEmptyStateAndHandlesEdgeCases`: Empty heap size, isEmpty, and null polling behavior.
  2. `singleElementHeapMaintainsInvariants`: Single-element push and extraction.
  3. `ranksByDescendingRiskScore`: Priority extraction in strict descending order of risk score.
  4. `deterministicTieBreakingUsesAscendingEncounterId`: Secondary tie-breaking by ascending encounter ID.
  5. `higherRiskScoreAlwaysOutranksSmallerId`: Primary score ordering strictly dominates secondary ID ordering.
  6. `handlesIncreasingAndDecreasingSequences`: Heap property preservation across monotonic input sequences.
  7. `dynamicResizingAndManyElementsMaintainHeapOrder`: Automatic array capacity doubling under large workloads.
  8. `toSortedListDrainsWithoutMutatingOriginalHeap`: Non-destructive sorted list export.
  9. `heapifyConstructorBuildsValidHeapInLinearTime`: $O(N)$ bottom-up heap construction from array.
  10. `clearEmptiesHeap`: Heap reset and size clearing.
  11. `rejectsNullAndUnassessedEncounters`: Guard rejecting null encounters and unassessed status.
  12. `rejectsInvalidInitialCapacity`: Guard rejecting non-positive initial capacity.
  13. `rebuildRestoresHeapInvariantsAfterExternalScoreMutation`: In-place heap restoration via `rebuild()` after external updates.
* **`CustomStackTest` (6 tests):**
  1. `emptyStackBehavior`: Empty stack size, isEmpty, and exception on empty pop/peek.
  2. `pushAndPopSingleElement`: Single element push, peek, and pop operations.
  3. `maintainsLIFOOrder`: Strict last-in-first-out element retrieval order.
  4. `clearEmptiesStack`: Stack reset and capacity preservation.
  5. `rejectsNullItem`: Guard rejecting null element insertion.
  6. `maneuverHistoryUndoWorkflow`: Application exhibit: sequential maneuver undo history tracking.
* **`CustomQueueTest` (6 tests):**
  1. `emptyQueueBehavior`: Empty queue size, isEmpty, and exception on empty dequeue/peek.
  2. `enqueueAndDequeueSingleElement`: Single element enqueue, peek, and dequeue operations.
  3. `maintainsFIFOOrder`: Strict first-in-first-out element retrieval order.
  4. `clearEmptiesQueue`: Circular buffer reset and size clearing.
  5. `rejectsNullItem`: Guard rejecting null element insertion.
  6. `telemetryEventStreamWorkflow`: Application exhibit: chronological telemetry event stream buffering.
* **`CustomSorterTest` (6 tests):**
  1. `sortsDoubleArrayIncludingNegativesAndDuplicates`: In-place merge sort on primitives including negative and duplicate values.
  2. `sortsIntArrayIncludingNegativesAndDuplicates`: In-place integer merge sort with duplicate handling.
  3. `sortsGenericListWithComparator`: Generic list sorting using custom `Comparator`.
  4. `sortsEncountersByRiskScoreDescendingWithDeterministicTieBreak`: Domain sorting: descending risk score with ascending ID tie-break.
  5. `sortsEncountersBySeparationAscending`: Domain sorting: ascending minimum separation distance.
  6. `preservesStabilityOnEqualKeys`: Stable sorting guarantee on identical keys.
* **`OrbitalGraphTest` (7 tests):**
  1. `emptyGraphState`: Empty graph vertex count, edge count, and empty adjacency lookups.
  2. `addVerticesAndUndirectedEdges`: Vertex registration and undirected edge insertion.
  3. `validationRejectsInvalidInputs`: Guards rejecting null vertices, duplicate self-loops, and invalid edges.
  4. `bfsTraversesLevelByLevel`: Breadth-first search level-order vertex traversal.
  5. `bfsCalculatesShortestHopDistance`: Shortest unweighted hop distance calculation via BFS.
  6. `dfsTraversesBranchesAndHandlesCycles`: Depth-first search branch exploration and cycle avoidance.
  7. `findsConnectedRiskClustersFromSimulationEncounters`: Application exhibit: connected component analysis for encounter threat clusters.

### 4.8 Performance Benchmarking (1 class, 4 tests)
* **`SpatialGridBenchmarkTest` (4 tests):**
  1. `executesBenchmarkDeterministicallyForIdenticalInputs`: Benchmark measurement reproducibility.
  2. `verifiesCorrectnessAndSignificantCandidateReduction`: Broad-phase candidate reduction efficiency vs naive pairwise.
  3. `rejectsInvalidObjectCountsAndNullInputs`: Parameter validation for benchmark runs.
  4. `enforcesBenchmarkResultValidationAndExposesFields`: Typed result model validation.

### 4.9 Engine Facade, Determinism & Boundaries (1 class, 11 tests)
* **`OrbitalEngineTest` (11 tests):**
  1. `executesBaselineRunSuccessfullyWithEncountersAndThreatRanking`: End-to-end baseline orchestration.
  2. `plansManeuversAndReplaysViaFacade`: Facade maneuver planning and retrospective replay.
  3. `verifiesManeuverDisplacementOracleAtNonZeroDetectionTick`: Kinematic displacement oracle at tick 10.
  4. `verifiesBaselineImmutabilityAcrossRetrospectiveReplay`: Full snapshot verification of baseline immutability.
  5. `rejectsInvalidManeuverReplayInputAssociationsInFacade`: Replay input validation through facade.
  6. `replayProducesDeterministicResultsAcrossRepeatedExecutions`: Facade replay determinism.
  7. `generatesScenariosAndRunsBenchmarksViaFacade`: Benchmarking execution through facade.
  8. `rejectsInvalidInputsAndSurfacesErrorsConsistently`: Exception translation and error surfacing.
  9. `marksRunFailedAndRethrowsWhenRuntimeExceptionOccursDuringTickProcessing`: Mid-simulation runtime failure handling.
  10. `executesNoThreatScenarioProducingZeroEncountersAndEmptyRankings`: Engine-level no-threat verification.
  11. `executesBaselineRunDeterministicallyAcrossRepeatedInvocations`: Seeded baseline determinism across runs.

---

## 5. Phase 14 Completion Verification (Criteria 1–7)

| Phase 14 Criterion | Requirement | Verification Evidence |
|---|---|---|
| **Criterion 1** | Major-class JUnit coverage | 27 test classes covering 33 production classes; 215 tests passing. |
| **Criterion 2** | Required edge cases | Zero objects, 1 object, duplicate IDs, identical positions, zero relative velocity, negative coords, invalid candidate associations covered by explicit tests. |
| **Criterion 3** | Deterministic results | Proved in `OrbitalEngineTest` (baseline) and `ManeuverReplayServiceTest` (replay). |
| **Criterion 4** | No-threat behavior | Proved in `OrbitalEngineTest.executesNoThreatScenarioProducingZeroEncountersAndEmptyRankings`. |
| **Criterion 5** | End-to-end core workflow | Proved in `OrbitalEngineTest.plansManeuversAndReplaysViaFacade` with baseline immutability verified. |
| **Criterion 6** | Formatting and static checks | Pure Java SE maintained; zero framework, HTTP, or database dependencies. |
| **Criterion 7** | SE test evidence ready | Fully cataloged in this document (`docs/MEMBER1_TEST_EVIDENCE.md`). |

---

## 6. Integration Handover Notes for Member 3

1. **Test Plan Ingestion:** Member 3 can copy Section 3 directly into the **System Test Cases / Unit Test Execution Matrix** section of the Software Engineering Test Plan.
2. **Traceability to Requirements:** The 9 subsystems defined in Section 2 map directly to the Core Engine modules in `PROJECT_CONTEXT.md` §18.
3. **Reproducibility:** All tests can be reproduced on any machine running Java 21 and Maven using:
   ```powershell
   mvn -f .\orbitalnet-core\pom.xml test -DargLine="-Xmx256m"
   ```
4. **Integration Boundary Reminder:** Integration testing with `orbitalnet-service` (REST endpoints) and `db/` (JDBC persistence) will occur on the `dev` branch once Member 2 and Member 3 merge their respective milestone branches.

