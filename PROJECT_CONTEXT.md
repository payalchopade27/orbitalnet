# OrbitalNet — Project Context

## 1. Project Overview

**Project name:** OrbitalNet

**Purpose:** Build an educational space-traffic simulation system that models satellites and space debris, detects close approaches, estimates collision risk, ranks threats, and suggests possible avoidance maneuvers.

**Academic areas covered:**
- Java and Object-Oriented Programming
- Fundamentals of Data Structures (FODS)
- Algorithms and simulation
- SQL and database management
- Software Engineering, design, documentation, and testing
- REST API and frontend development

This project is being developed by a team of three members using AI-assisted development. Every team member must understand and be able to explain the code they contribute.

## 2. Team Responsibilities

### Member 1 — Core Engine and FODS
Responsible for:
- Core domain model and simulation engine
- Data structures and algorithms
- Space-object movement and close-approach detection
- Risk scoring and threat ranking
- Maneuver candidate generation
- Algorithm correctness and performance benchmarking

### Member 2 — Service and UI
Responsible for:
- Spring Boot backend
- REST controllers, services, DTOs, and exception handling
- API implementation
- React and Three.js frontend
- Dashboard, simulation visualization, and user interaction
- Frontend integration with backend APIs

### Member 3 — Data and Software Engineering
Responsible for:
- Database design and SQL schema
- Seed/sample data
- Analytical SQL queries
- JDBC repositories and database integration support
- Repository testing and advanced SQL demonstrations
- SRS, UML diagrams, test plan, risk register, and process documentation
- Postman collection and integration-testing support

**Shared responsibility:** All members must agree on shared contracts, review integration changes, and understand the overall architecture.

## 3. Current Repository

**GitHub repository:** https://github.com/payalchopade27/orbitalnet

**Default branch:** `main`

**Integration branch:** `dev`

**Planned working branches:**
- `member1-core`
- `member2-service-ui`
- `member3-data-se`

The team should verify that all branches exist on GitHub before relying on them.

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

### Folder responsibilities

- `orbitalnet-core/`: Java domain model, simulation logic, FODS, and algorithms.
- `orbitalnet-service/`: Spring Boot application and REST API.
- `frontend/`: React/Three.js user interface.
- `db/`: SQL schema, seed data, and analytical queries.
- `docs/api/`: API specification and sample request/response JSON.
- `docs/srs/`: Software Requirements Specification.
- `docs/uml/`: UML and architecture diagrams.
- `docs/testing/`: Test plan, test cases, and test evidence.
- `docs/project-management/`: Risk register, process model, and AI usage log.
- `PROJECT_CONTEXT.md`: Shared project context and agreed decisions.
- `pom.xml`: Root Maven configuration for the Java modules.

**Status:** This is the planned structure. Some files and folders may still be placeholders. Empty directories must contain tracked files, such as `.gitkeep`, if they need to appear in Git.

## 5. Intended Architecture

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

The exact dependency and data flow will be finalized during integration.

The core engine is responsible for simulation and calculations. The service layer exposes application functionality through APIs. Repositories handle persistence. The frontend displays data and lets users interact with the simulation.

## 6. Planned Domain Model

The project guide identifies these initial domain classes:

- `Vector3D`
- `SpaceObject`
- `Satellite`
- `Debris`
- `Orbit`
- `Encounter`
- `RiskScore`
- `ManeuverCandidate`

Member 1 will propose the exact fields, types, constructors, and relationships. All three members must review and agree on the contract before implementation proceeds independently.

Do not invent or change field names and types without communicating with the team.

## 7. API Contract

**Planned specification:** `docs/api/openapi.yaml`

Member 2 will propose the endpoints, HTTP methods, request and response formats, parameters, and error responses.

The three members must agree on the API contract before the frontend and backend independently implement it.

Sample API responses will be stored under `docs/api/samples/` so frontend work can begin before the real backend is available.

**Status:** Exact endpoints and JSON schemas are pending agreement.

## 8. Database Contract

**Schema file:** `db/schema.sql`

**Seed file:** `db/seed.sql`

Member 3 will propose:
- Tables and columns
- SQL data types
- Primary and foreign keys
- Constraints and relationships
- Normalization decisions
- Required analytical queries

The database must represent the agreed domain model and support the application's use cases.

The exact schema is pending review and agreement by all three members.

## 9. Technology Stack

The intended stack includes:

- Java
- Maven
- Spring Boot
- SQL database
- JDBC or JdbcTemplate
- React
- Three.js
- JUnit

The exact versions, SQL database product, package names, and build configuration must be confirmed by the team and recorded here before implementation. Do not assume unconfirmed versions.

## 10. Git Workflow

- `main` is the stable branch.
- `dev` is the shared integration branch.
- Each member develops on their own working branch.
- Commit small, understandable changes.
- Pull the latest changes before beginning work.
- Use pull requests or an agreed review process to merge into `dev`.
- Test integration changes before promoting them to `main`.
- Never use force-push or overwrite another member's work without team agreement.

Suggested commit messages:
- `docs: add project context`
- `db: add initial schema`
- `core: implement spatial index`
- `service: add object endpoint`
- `test: add repository tests`

## 11. Development Rules

1. Freeze shared contracts before independent implementation.
2. Work on one small task at a time.
3. Understand AI-generated code before committing it.
4. Do not commit code that cannot be explained.
5. Write and test SQL rather than blindly accepting generated queries.
6. Add tests for important functionality and edge cases.
7. Keep API, domain model, and database changes coordinated.
8. Update documentation when an agreed design changes.
9. Maintain an AI usage log where appropriate for academic assessment.
10. Use sample data or mocks when another component is not ready.

## 12. Simplified Simulation Assumptions

The simulation models space objects and estimates close approaches and collision risk for educational purposes.

The exact movement model, units, simulation time step, coordinate conventions, risk-scoring formula, and maneuver assumptions must be explicitly agreed upon and documented before implementation.

Do not present simplified simulation outputs as operationally accurate real-world collision predictions.

## 13. Current Progress

- GitHub repository created.
- Initial Git commit created.
- Git branches are being configured.
- Initial project folders and placeholder files are being prepared.
- Shared domain, API, and database contracts are not yet confirmed.
- Maven build configuration is not yet finalized.
- Independent implementation should begin only after the required shared contracts are agreed upon.

Update this section as the project progresses.

## 14. Immediate Next Steps

1. Verify `main`, `dev`, and all three working branches on GitHub.
2. Commit and push the common project structure and this context file to `dev`.
3. Confirm Java, Maven, database, frontend, and package conventions.
4. Have Member 1 propose the domain model.
5. Have Member 2 propose the API contract.
6. Have Member 3 propose the database schema.
7. Review the three contracts together and resolve inconsistencies.
8. Commit the agreed contracts.
9. Begin independent development using the frozen contracts.

**This document is the shared source of truth. If an important project decision changes, discuss it with the team and update this file.**
