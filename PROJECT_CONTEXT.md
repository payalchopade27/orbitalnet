# OrbitalNet — Project Context

## 1. Project Overview

**Project name:** OrbitalNet

**Purpose:** Build an educational space-traffic simulation system that models satellites and space debris, detects close approaches, estimates collision risk, ranks threats, and evaluates possible avoidance maneuvers.

**Academic areas covered:**

- Java and Object-Oriented Programming
- Fundamentals of Data Structures (FODS)
- Algorithms and simulation
- SQL and database management
- Software Engineering, design, documentation, and testing
- REST API and frontend development

This project is being developed by a team of three members using AI-assisted development. Every team member must understand and be able to explain the code they contribute.

This project is an **educational simulation**. It does not provide real-world orbital mechanics, operational collision probability, or mission-safety predictions.

---

## 2. Team Responsibilities

### Member 1 — Core Engine and FODS

Responsible for:

- Core domain model and simulation engine
- Data structures and algorithms
- Space-object movement and close-approach detection
- Closest-approach calculation
- Risk scoring and threat ranking
- Maneuver candidate generation and evaluation
- Algorithm correctness and performance benchmarking

### Member 2 — Service and UI

Responsible for:

- Spring Boot backend
- REST controllers, services, DTOs, and exception handling
- API implementation
- React and Three.js frontend
- Dashboard and simulation visualization
- User interaction
- Frontend integration with backend APIs
- OpenAPI specification and API samples

### Member 3 — Data and Software Engineering

Responsible for:

- Database design and SQL schema
- Seed/sample data
- Analytical SQL queries
- JDBC repositories and database integration support
- Repository testing and advanced SQL demonstrations
- SRS, UML diagrams, test plan, risk register, and process documentation
- Postman collection
- Integration-testing support
- Database persistence and retrieval consistency

### Shared Responsibility

All members must:

- Follow the frozen shared implementation contract.
- Review changes that affect shared contracts.
- Coordinate domain, API, and database changes.
- Test integration changes.
- Understand the overall architecture.
- Understand and explain AI-assisted code before committing it.

---

## 3. Repository and Git Workflow

**GitHub repository:**

https://github.com/payalchopade27/orbitalnet

**Stable branch:** `main`

**Integration branch:** `dev`

**Working branches:**

- `member1-core`
- `member2-service-ui`
- `member3-data-se`

Each member should work primarily on their own branch and integrate changes through `dev`.

### Git Rules

- Pull/fetch the latest changes before starting work.
- Commit small, understandable changes.
- Do not overwrite another member's work.
- Do not force-push shared branches.
- Review integration changes before merging.
- Test after integration.
- Promote stable, tested work from `dev` to `main`.

Suggested commit messages:

- `docs: update project context`
- `db: add initial schema`
- `core: implement spatial grid`
- `service: add simulation endpoint`
- `test: add repository tests`

---

## 4. Project Structure

The planned repository structure is:

```text
orbitalnet/
├── pom.xml
├── PROJECT_CONTEXT.md
│
├── orbitalnet-core/
│   └── src/
│       ├── main/java/
│       └── test/java/
│
├── orbitalnet-service/
│   └── src/
│       ├── main/java/
│       └── test/java/
│
├── frontend/
│
├── db/
│   ├── schema.sql
│   ├── seed.sql
│   └── queries/
│
└── docs/
    ├── api/
    │   ├── openapi.yaml
    │   └── samples/
    ├── srs/
    ├── uml/
    ├── testing/
    └── project-management/
```

### Folder Responsibilities

- `orbitalnet-core/`: Java domain model, simulation logic, FODS, algorithms, detection, risk scoring, and maneuver logic.
- `orbitalnet-service/`: Spring Boot application, REST API, DTOs, services, and repositories.
- `frontend/`: React/Three.js user interface.
- `db/`: SQL schema, seed data, and analytical queries.
- `docs/api/`: OpenAPI specification and sample request/response JSON.
- `docs/srs/`: Software Requirements Specification.
- `docs/uml/`: UML and architecture diagrams.
- `docs/testing/`: Test plan, test cases, and test evidence.
- `docs/project-management/`: Risk register, process model, and AI usage log.
- `PROJECT_CONTEXT.md`: Shared project context and frozen implementation decisions.
- `pom.xml`: Root Maven configuration for the Java modules.

Some files or folders may still be placeholders. Empty directories must contain tracked files such as `.gitkeep` if they need to appear in Git.

---

## 5. Architecture

```text
React + Three.js Frontend
            |
            | HTTP / JSON
            v
Spring Boot REST API
            |
            v
Service Layer
       /          \
      v            v
Core Engine     JDBC Repositories
      |            |
      v            v
Simulation      SQL Database
and Algorithms
```

### Core Principle

The **core engine is independent of Spring Boot, HTTP, and the database**.

The core engine performs:

- Object movement
- Spatial indexing
- Closest-approach calculations
- Encounter detection
- Risk scoring
- Threat ranking
- Maneuver evaluation
- Simulation logic

The service layer exposes the core functionality through REST APIs.

Repositories handle database persistence and retrieval.

The frontend consumes the REST API and visualizes simulation results.

---

# 6. Frozen Shared Implementation Contract

The following decisions are **approved MVP baseline decisions** and must be treated as the shared source of truth.

Do not silently change these decisions in individual branches.

If a real implementation blocker is discovered, discuss it with the team before changing the contract.

---

## 6.1 Units and Time

The project uses:

- Position: **kilometers (km)**
- Velocity: **kilometers per second (km/s)**
- Delta-v: **kilometers per second (km/s)**
- Time step: **5 seconds**
- Elapsed time: **seconds**
- Time to closest approach: **seconds**
- Minimum separation: **kilometers**
- Collision radius / object size: **kilometers**
- Relative velocity: **km/s**
- Risk score: **0–100**

Application-generated Java `String` IDs are used for persistent entities.

IDs must remain stable during a simulation run.

---

## 6.2 Coordinate and Movement Model

Orbital objects use a **3D Cartesian coordinate system**.

For normal simulation movement:

```text
newPosition = oldPosition + velocity × dt
```

where:

```text
dt = 5 seconds
```

The MVP uses constant velocity propagation.

Real orbital mechanics are **not** implemented.

`Orbit` is retained as metadata only.

The prediction horizon is:

```text
3600 seconds
```

All simulation calculations must use these agreed assumptions consistently.

---

# 7. Domain Model

The agreed initial domain classes are:

- `Vector3D`
- `SpaceObject`
- `Satellite`
- `Debris`
- `Orbit`
- `Scenario`
- `SimulationRun`
- `Encounter`
- `RiskScore`
- `ManeuverCandidate`

---

## 7.1 Vector3D

`Vector3D` represents a 3D vector.

Fields:

```text
x
y
z
```

The vector is immutable.

Required operations:

- `add`
- `subtract`
- `scale`
- `dot`
- `magnitude`
- `distanceTo`

---

## 7.2 SpaceObject

`SpaceObject` is an abstract base class.

Fields:

```text
id
type
position
velocity
size
status
```

Object types:

```text
SATELLITE
DEBRIS
```

Object statuses:

```text
ACTIVE
INACTIVE
DESTROYED
```

Only active objects participate in close-approach calculations.

---

## 7.3 Satellite

`Satellite` extends `SpaceObject`.

Additional fields include:

```text
name
optional Orbit
```

---

## 7.4 Debris

`Debris` extends `SpaceObject`.

It may contain:

```text
sourceType
```

`sourceType` is optional.

---

## 7.5 Orbit

`Orbit` contains metadata such as:

```text
altitudeKm
inclinationDeg
periodSeconds
```

Orbit data is metadata only in the MVP.

It is not used for full orbital propagation.

---

## 7.6 Scenario

A `Scenario` contains:

- Scenario ID
- Generation configuration
- Seed
- Initial object definitions/states
- Scenario status

Scenario status:

```text
READY
FAILED
```

The scenario must retain enough information to reproduce the initial conditions.

---

## 7.7 SimulationRun

A `SimulationRun` represents one execution of a scenario.

It contains:

- Run ID
- Scenario ID
- Status
- Current tick
- Elapsed time
- Reproducible initial-condition reference
- Final object states
- Final simulation results
- Optional baseline run ID
- Optional maneuver candidate ID

Run statuses:

```text
PENDING
RUNNING
COMPLETED
FAILED
```

A maneuver application creates a **new simulation run**.

The baseline run must never be overwritten.

---

# 8. Encounter Model

An `Encounter` represents a close-approach event between exactly two objects.

Fields include:

```text
encounterId
runId
objectAId
objectBId
minSeparationKm
timeToClosestApproachSeconds
relativeVelocityKmPerSec
detectedAtTick
status
latest RiskScore
```

Object pairs use canonical ordering.

There is **no unique constraint only on**:

```text
runId + objectAId + objectBId
```

because the same pair may have multiple distinct encounter events during a run.

If the same pair continues the same unresolved event, the encounter is updated.

A later distinct event creates a new encounter.

`detectedAtTick` is immutable once the encounter is created.

Encounter lifecycle:

```text
DETECTED
ASSESSED
MITIGATED
RESOLVED
```

Resolved encounters remain stored and are not deleted.

---

# 9. Closest-Approach Calculation

All components must use the same closest-approach calculation.

Given:

```text
r = positionB - positionA
v = velocityB - velocityA
```

Calculate:

```text
t* = -(r · v) / ||v||²
```

Then clamp:

```text
t* ∈ [0, 3600]
```

If relative velocity is zero:

```text
t* = 0
```

Closest separation is:

```text
rClosest = r + v × t*
minSeparation = ||rClosest||
```

Relative velocity is:

```text
relativeVelocity = ||v||
```

Only active objects participate.

An encounter is detected when:

```text
minSeparation <= 100 km
```

A collision condition is:

```text
minSeparation <= sizeA + sizeB
```

---

# 10. Risk Scoring

Risk detection parameters:

```text
Detection distance = 100 km
Prediction horizon = 3600 seconds
Velocity normalization = 15 km/s
```

### Separation Factor

```text
separationFactor = clamp(1 - minSeparationKm / 100)
```

### Velocity Factor

```text
velocityFactor = clamp(relativeVelocityKmPerSec / 15)
```

### Time Factor

```text
timeFactor = 1 - clamp(timeToClosestApproachSeconds / 3600)
```

### Total Risk

```text
total =
round(
    100 *
    (
        0.50 * separationFactor
        + 0.30 * velocityFactor
        + 0.20 * timeFactor
    )
)
```

Risk range:

```text
0–100
```

Risk labels:

```text
0–24   Low
25–49  Moderate
50–74  High
75–100 Critical
```

The risk explanation should clearly communicate the score and factors.

Recommended format:

```text
Risk {total}/100: separation factor {separationFactor}, velocity factor {velocityFactor}, time factor {timeFactor}. Weights: separation 50%, velocity 30%, time 20%.
```

The same risk formula must be used by the core engine, API, and persistence layer.

The API must not calculate a different risk score from the core engine.

---

# 11. FODS Requirements

The project must demonstrate custom data structures where required.

Required structures include:

### Custom Hash Table

Used for efficient object ID lookup.

Expected average lookup:

```text
O(1)
```

### Spatial Grid

Used to reduce unnecessary object-pair checks by considering nearby spatial cells.

### Custom Heap

Used to rank assessed encounters by risk.

Tie-breaking must be deterministic.

### Additional Structures

Where required by the project:

- Graph adjacency list
- BFS/DFS
- Stack for maneuver history/replay
- Queue for telemetry/events
- Sorting for threats, scenarios, and benchmark results

Do not replace the assessed custom FODS with Java `HashMap` or `PriorityQueue` when the custom structure is part of the academic requirement.

---

# 12. Maneuver Model

A `ManeuverCandidate` contains:

```text
maneuverCandidateId
encounterId
targetObjectId
deltaV
newMinSeparationKm
cost
rank
```

There may be multiple candidates for one encounter.

`rank` must be unique within an encounter.

Maneuver candidates must be generated and evaluated deterministically.

### Applying a Maneuver

Applying a maneuver:

1. Validates the encounter.
2. Validates the candidate.
3. Validates that the candidate belongs to the encounter.
4. Validates the baseline run.
5. Replays the baseline through `detectedAtTick`.
6. Applies the candidate delta-v.
7. Continues the remaining forecast.
8. Uses the same 5-second timestep.
9. Uses the same closest-approach and risk rules.
10. Saves the result as a separate simulation run.

The baseline run is preserved.

The new run stores:

```text
baselineRunId
maneuverCandidateId
```

Where practical, duplicate maneuver-run creation caused by retries should be prevented through an agreed idempotency or uniqueness mechanism.

---

# 13. API Contract

The API specification is maintained in:

```text
docs/api/openapi.yaml
```

The API uses:

- Stable string IDs
- `camelCase` JSON fields
- Standard HTTP status codes
- A consistent error response format

### Main Endpoints

```text
POST /api/scenarios/generate

GET /api/scenarios

POST /api/simulations/{scenarioId}/run

GET /api/simulations/{runId}/state

GET /api/simulations/{runId}/encounters

GET /api/encounters/{encounterId}

POST /api/encounters/{encounterId}/maneuvers

POST /api/encounters/{encounterId}/maneuvers/{maneuverCandidateId}/apply

GET /api/benchmarks
```

### Simulation State

The state endpoint returns the **latest complete snapshot**.

It does not promise historical tick access.

There is no:

```text
?tick=n
```

historical-state contract.

Before a run starts, a snapshot may be unavailable.

While running, the endpoint exposes the latest available state.

After completion, it exposes the final state.

### Maneuver API

Generating maneuver candidates does not modify the baseline run.

Applying a candidate creates a new run.

The API must expose the relationship between:

```text
new run
baseline run
maneuver candidate
```

### API Validation

The API must correctly handle:

- Unknown IDs
- Invalid scenario IDs
- Invalid run IDs
- Invalid encounter IDs
- Wrong candidate ownership
- Invalid baseline relationships
- Invalid maneuver applications
- State unavailable before run start

---

# 14. Database Contract

The database must persist enough information to support:

- Scenario reproduction
- Simulation runs
- Final object states
- Encounters
- Risk assessments
- Maneuver candidates
- Maneuver simulation relationships
- Benchmark results
- Required event information

Planned core tables:

```text
scenario
space_object
simulation_run
encounter
risk_assessment
maneuver_candidate
benchmark_result
event_log
```

### Important Database Rules

- Use stable application-generated string IDs.
- Maintain correct primary and foreign keys.
- Keep optional Orbit information consistent with the domain model.
- `detectedAtTick` must remain immutable.
- Resolved encounters must be retained.
- Do not create a unique constraint that prevents multiple distinct encounters for the same object pair in a run.
- `rank` must be unique within an encounter.
- Maintain one current/latest risk assessment per encounter according to the agreed design.
- Do not create a risk row for every simulation tick.
- Do not persist every object state for every tick.
- Store enough initial information to reproduce a run.
- Store final states and final results.
- Maneuver runs must reference their baseline run and maneuver candidate.
- Avoid circular database references.
- Database lifecycle/status values must remain consistent with the domain and API.

---

# 15. Reproducibility

Simulation runs must be reproducible from:

- Initial scenario conditions
- Scenario configuration
- Seed
- Fixed simulation parameters
- 5-second timestep
- Agreed detection parameters
- Agreed risk formula

For maneuver replay:

1. Reproduce the baseline through the encounter's detection tick.
2. Apply the selected delta-v.
3. Continue the remaining forecast.
4. Use the same simulation rules.

Historical per-tick snapshots are **not required**.

If reproducibility problems are discovered, fix the implementation rather than silently changing the shared contract.

---

# 16. Database and Software Engineering Responsibilities

Member 3 must ensure:

- Schema integrity
- Correct PK/FK relationships
- Constraint validation
- Persistence and retrieval
- Latest risk update
- Retention of resolved encounters
- Maneuver-run relationships
- Reproducibility support
- API/domain/database consistency

Required SQL work may include:

- Analytical queries
- Views
- Indexes
- Triggers
- Stored procedures
- Transactions
- Repository queries
- Database tests

All SQL must be understandable and testable.

---

# 17. Testing Responsibilities

### Member 1

Tests:

- Core domain classes
- Vector operations
- Simulation movement
- Custom FODS
- Closest-approach calculation
- Encounter detection
- Risk scoring
- Heap ordering
- Maneuver evaluation
- Edge cases

### Member 2

Tests:

- REST endpoints
- Request validation
- Response structure
- Error handling
- Service integration
- Frontend/API integration

### Member 3

Tests:

- Database schema
- PK/FK integrity
- Constraints
- Persistence and retrieval
- Latest risk update
- Resolved encounter retention
- Maneuver relationships
- Reproducibility-related database data
- Repository behavior
- SQL queries

Integration testing is a shared responsibility.

---

# 18. Out of Scope for MVP

The following are not part of the MVP:

- Real orbital mechanics
- Real-world tracking data
- Real-time satellite tracking
- Machine learning
- Per-tick database persistence
- Historical tick-state API
- Pause/resume/cancel simulation
- Debris breakup simulation
- Advanced graph features unless specifically required
- Fully animated 3D Earth

Nice-to-have features may be added only after the MVP is stable.

---

# 19. Educational MVP Goal

The MVP should demonstrate:

- Java OOP
- Custom FODS
- Deterministic simulation
- Satellite and debris movement
- Spatial indexing
- Close-approach detection
- Explainable risk scoring
- Heap-based threat ranking
- Maneuver candidate generation
- At least one maneuver simulation/comparison
- SQL persistence
- REST API
- Postman testing
- Basic frontend visualization
- Benchmarking of naive pairwise checking versus spatial indexing

The simulation is intended for academic demonstration, not operational use.

---

# 20. Current Project Status

The shared implementation contract has been approved.

### Completed / Agreed

- Project scope agreed
- Team responsibilities defined
- Repository structure planned
- Domain contract frozen
- Units and coordinate model frozen
- Simulation timestep frozen at 5 seconds
- Prediction horizon frozen at 3600 seconds
- Closest-approach calculation frozen
- Detection threshold frozen at 100 km
- Risk formula frozen
- Encounter lifecycle defined
- Maneuver workflow defined
- API behavior agreed
- Database requirements agreed
- Reproducibility rules agreed
- Testing responsibilities defined
- MVP out-of-scope items defined

### Current Work

The team can now proceed with implementation using the frozen contract.

Member 1 can implement the core engine and FODS.

Member 2 can implement the Spring Boot service, API, and frontend.

Member 3 can implement the database schema, seed data, SQL queries, repositories, and software-engineering documentation.

---

# 21. Implementation Order

Recommended implementation order:

### Phase 1 — Foundation

- Verify repository structure.
- Verify Maven modules.
- Confirm package conventions.
- Confirm database connection configuration.
- Confirm API/OpenAPI files.
- Confirm shared contract is committed.

### Phase 2 — Core Simulation

Member 1:

- Implement `Vector3D`.
- Implement `SpaceObject`, `Satellite`, and `Debris`.
- Implement simulation movement.
- Implement custom hash table.
- Implement spatial grid.
- Implement closest-approach calculation.
- Implement custom heap.
- Implement encounter detection.
- Implement risk scoring.

### Phase 3 — Database

Member 3:

- Finalize ER model.
- Implement `db/schema.sql`.
- Implement `db/seed.sql`.
- Add constraints and indexes.
- Add analytical queries.
- Implement JDBC repositories.
- Add repository/database tests.

### Phase 4 — Service/API

Member 2:

- Implement Spring Boot service layer.
- Implement DTOs.
- Implement controllers.
- Implement API validation.
- Connect repositories.
- Connect the core engine.
- Test API using Postman.

### Phase 5 — Maneuvers and Benchmarking

- Generate maneuver candidates.
- Apply maneuvers using separate simulation runs.
- Preserve baseline runs.
- Implement benchmark comparison.
- Persist required results.

### Phase 6 — Frontend

- Basic 2D simulation visualization.
- Scenario generation.
- Simulation state display.
- Threat dashboard.
- Encounter detail.
- Risk explanation.
- Maneuver comparison.
- Algorithm Lab / benchmark display.

### Phase 7 — Integration and Testing

- Merge components through `dev`.
- Test end-to-end workflow.
- Verify API/database/core consistency.
- Verify reproducibility.
- Test edge cases.
- Fix integration issues.
- Prepare demonstration and viva material.

---

# 22. Change Governance

This document and the approved shared implementation contract are the project's shared source of truth.

The following must not be changed silently:

- Domain fields
- Units
- Simulation timestep
- Detection threshold
- Risk formula
- Encounter lifecycle
- Maneuver workflow
- API behavior
- Database relationships
- Reproducibility rules
- MVP scope

If an actual implementation blocker requires a contract change:

1. Explain the blocker.
2. Discuss it with the team.
3. Agree on the change.
4. Update the relevant source-of-truth documentation.
5. Update affected code/contracts.
6. Test the change across affected components.

Do not create conflicting assumptions in individual branches.

---

**OrbitalNet is now implementation-ready based on the approved MVP shared contract.**
