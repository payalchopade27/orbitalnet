# OrbitalNet Integration Handoff Document

**Target Branch:** `test-member3-integration`  
**Repository:** `https://github.com/payalchopade27/orbitalnet`  
**Date:** November 2026  
**Status:** Integration Verified & Complete (Ready for Frontend Feature Implementation)

---

## 1. Project Purpose & Architecture

OrbitalNet is an educational space-traffic simulation platform designed to demonstrate satellite motion tracking, close-approach encounter detection, collision risk scoring, and avoidance maneuver evaluation.

### Architecture Overview
The system consists of three architectural tiers:
1. **`orbitalnet-core` (Domain Engine):**
   * Pure Java 21 numerical propagation and collision detection library.
   * Implements Spatial Grid indexing ($O(N)$ candidate detection), physics vectors, heuristic risk scoring, and delta-V maneuver calculation.
   * **Boundary Rule:** Must remain completely independent of Spring Boot, HTTP, and database frameworks.
2. **`backend` (REST API & Persistence Layer):**
   * Spring Boot 4.0 application managing simulation sessions, REST controllers, and database interactions.
   * Connects to Supabase PostgreSQL using connection pooling and standard JDBC batching.
   * Manages simulation lifecycle: scenario generation, run instantiation, tick progression, encounter persistence, and maneuver execution.
3. **`frontend` (Dashboard UI):**
   * Single-page React 19 application built with Vite 8.
   * Provides real-time dashboard controls to step simulations, monitor tracked objects, filter close approaches, and view illustrative 2D projections.
   * **Note on Visualization:** The 2D Earth and orbit canvas provides an **illustrative, educational demo view**, not high-precision real-world orbital mechanics.

---

## 2. Team Responsibilities & Boundaries

* **Member 1 (Simulation Core):** Maintained `orbitalnet-core`. All 215 core unit tests pass cleanly. Core contracts are frozen and must not be coupled to backend or frontend frameworks.
* **Member 2 (Frontend Interface):** Built the baseline React + Vite dashboard. Responsible for UI layout, CSS theming, and real-time polling/stepping controls.
* **Member 3 (Backend Integration & Persistence - This Handoff):** Integrated the Spring Boot backend with Supabase PostgreSQL, resolved primary key and foreign key constraint failures, scoped object IDs globally to scenarios, implemented deterministic object state reconciliation, restored persisted encounters on simulation retrieval, and validated the complete maneuver lifecycle.

> [!IMPORTANT]
> **Branch Workflow Rule:**  
> The next team member must branch from or continue directly on `test-member3-integration`.  
> **DO NOT** merge this branch into `main` or `dev` without explicit team authorization and code review.

---

## 3. Integration Changes in This Branch (`test-member3-integration`)

1. **Resolution of Foreign Key Failure on Simulation Advance:**
   * **Root Cause:** Encounter records inserted during `advanceRun()` referenced `space_object(object_id)`. However, generated space objects were only in memory and had not yet been committed to Supabase, triggering PostgreSQL foreign key violation `23503`.
   * **Resolution:** Added `ensureSpaceObjectsPersisted()` in `SimulationRunService` which guarantees all scenario objects exist in `space_object` prior to simulation run creation and advance steps.
2. **Scenario-Scoped Object ID Mapping:**
   * **Root Cause:** Core scenario generators previously produced identical local identifiers (`SAT-1`, `DEB-1`) across every scenario, colliding with the global primary key on `space_object.object_id`.
   * **Resolution:** Generated IDs now use full scenario UUID scoping:
     * Satellites: `SAT-<scenarioId>-<N>`
     * Debris: `DEB-<scenarioId>-<N>`
     * Generated length (~42–45 characters) easily fits the database column constraint `VARCHAR(100)`.
3. **Deterministic Physical-State Reconciliation:**
   * When loading an existing scenario, `SimulationRunService.createCoreScenario()` deterministically matches generated objects against existing database records:
     * Validates scenario ownership and object counts.
     * Matches by name and physical vectors ($\Delta \vec{x} < 0.05\text{ km}$, $\Delta \vec{v} < 0.05\text{ km/s}$).
     * Reuses the existing database `object_id`, ensuring existing records are never altered or overwritten.
4. **Idempotent Batch Persistence & Cross-Scenario Ownership Guard:**
   * `SpaceObjectRepository.saveAll()` utilizes:
     ```sql
     ON CONFLICT (object_id) DO UPDATE SET ... WHERE space_object.scenario_id = EXCLUDED.scenario_id
     ```
     `scenario_id` is omitted from the update `SET` clause, strictly preventing cross-scenario takeover.
5. **Simulation State Encounter Recovery:**
   * Extended `ActiveSession` with `restoredEncounters` map.
   * When restoring a simulation session from the database, persisted encounters and their associated risk assessments are loaded via `EncounterRepository` and `RiskAssessmentRepository` and merged into the response returned by `GET /api/simulations/{runId}/state`.
6. **Connection Pooler & Security Hardening:**
   * Migrated all repositories to the Supabase connection pooler (`aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require`).
   * Eliminated hardcoded credential fallbacks. Password is read exclusively via `System.getenv("SUPABASE_DB_PASSWORD")`.
   * Updated `.gitignore` to protect `.env` and `.env.*` files.

---

## 4. Database Schema & Test Artifact Evidence

### Supabase PostgreSQL Schema Dependencies
* `space_object`: `object_id` (PK, `VARCHAR(100)`), `scenario_id` (`VARCHAR(100)`), `source_type` (`VARCHAR(30)`, nullable), coordinates, velocity, orbital parameters.
* `simulation_run`: `run_id` (PK, `VARCHAR(100)`), `scenario_id`, `status`, `current_tick`, `elapsed_time_seconds`, `baseline_run_id`, `maneuver_candidate_id`.
* `encounter`: `encounter_id` (PK, `VARCHAR(100)`), `run_id` (FK &rarr; `simulation_run`), `object_a_id` (FK &rarr; `space_object`), `object_b_id` (FK &rarr; `space_object`).
* `risk_assessment`: `encounter_id` (PK/FK &rarr; `encounter`), score factors, heuristic total score (0–100).
* `maneuver_candidate`: `maneuver_candidate_id` (PK), `encounter_id` (FK &rarr; `encounter`), `target_object_id` (FK &rarr; `space_object`), delta-V coordinates, cost, rank.
* `final_object_state`: `run_id` (FK &rarr; `simulation_run`), `object_id` (FK &rarr; `space_object`), post-maneuver coordinates and velocity.

### Approved Test Artifacts Created
During verified integration test execution on scenario `56111e50-6fd9-4912-99b5-0c41e78a671e`, the following records were created in Supabase:
* **Baseline Test Run ID:** `6be3f475-8123-45e8-8089-573e6832c103` (Tick 1, Elapsed: 5.0s, Status: `RUNNING`)
* **Maneuver Run ID:** `d42872d3-32fc-4df1-8dcd-2399c26b9511` (Status: `COMPLETED`, Baseline: `6be3f475-8123-45e8-8089-573e6832c103`)
* **Tested Encounter ID:** `00fa6e51-3672-45ad-be8b-a64daf7deee8`
* **First Maneuver Candidate ID:** `00fa6e51-3672-45ad-be8b-a64daf7deee8-MAN-01`
* **Protected Run ID:** `0bce96c9-bae2-4361-9d80-9b3e8536b8b3` (Remained untouched at `PENDING`, tick 0).

> [!CAUTION]
> **No Database Cleanup Was Performed:**  
> These records serve as immutable verification evidence. **Do NOT run DELETE, TRUNCATE, or DROP scripts** against these records or tables.

---

## 5. Comprehensive Test Results Matrix

### A. Passed Tests
* **Core Unit Tests:** **215 / 215 passed** (`mvn test -pl orbitalnet-core`).
* **Backend Build & Compilation:** `mvn test-compile` succeeded across all modules (`orbitalnet-parent`, `orbitalnet-core`, `backend`).
* **Simulation Advance Integration Test:** **1 / 1 passed** (`SimulationAdvanceIntegrationTest`).
* **Encounter & Risk Assessment Persistence:** Verified 79 close encounters detected and 79 risk assessment rows persisted to Supabase during tick 1.
* **Maneuver Generation & Application:** Core `ManeuverPlanner` produced ranked candidates with delta-V vectors; `EncounterController.applyManeuver` persisted 18 `final_object_state` rows for the maneuver run.
* **Frontend Dependency Installation:** `npm ci` completed with **0 vulnerabilities**.
* **Frontend Production Build:** `npm run build` completed successfully without errors.
* **Frontend Interactive Lifecycle:**
  * Scenario generation (`POST /api/scenarios/generate`) verified.
  * Simulation initialization (`POST /api/simulations/{id}/run`) and state polling (`GET /state`) verified.
  * Manual step (`POST /step`) and automatic 1-second interval execution verified.
  * Encounter table rendering, risk sorting, and detail drill-down verified.

### B. Failed Tests
* **Frontend Refresh State Restoration:**  
  * While the backend properly persists scenarios and simulation runs in Supabase, refreshing the browser causes the React application state to reset to `"Ready to run"`.
  * The frontend does not currently query existing scenarios or restore `runId` from URL/local storage on page reload.

### C. Blocked / Skipped Tests
* **Automated CI Integration Test:** Running `SimulationAdvanceIntegrationTest` without network access or without `SUPABASE_DB_PASSWORD` set in the environment will fail due to database authentication requirements.

---

## 6. Known Issues & Gaps for the Next Team Member

### Missing Frontend Features (Priority for Next Member)
1. **Scenario Listing & Selection UI:**
   * Backend has `GET /api/scenarios` and `GET /api/scenarios/{id}`.
   * Frontend currently only has a form to generate new scenarios. Add a dropdown/selector to load existing scenarios from Supabase.
2. **Maneuver Planning UI:**
   * Backend has `POST /api/encounters/{id}/maneuvers`.
   * Add a "Plan Avoidance Maneuver" button inside the Encounter Details panel to display ranked candidates with delta-V costs and projected miss distances.
3. **Maneuver Application & Comparison View:**
   * Backend has `POST /api/encounters/{id}/maneuvers/{candidateId}/apply`.
   * Add a button to apply the selected maneuver and display post-maneuver trajectory/results from `final_object_state`.
4. **Spatial Grid Benchmark Visualization:**
   * Backend has `GET /api/benchmarks` and `POST /api/benchmarks/run`.
   * Add a dedicated panel or tab comparing naive $O(N^2)$ pairwise checking vs $O(N)$ spatial grid detection.

### Known Frontend & API Configuration Issues
1. **Hardcoded API URLs:**  
   `App.jsx` hardcodes `http://localhost:8080`. Migrate this to use `import.meta.env.VITE_API_URL || "http://localhost:8080"`.
2. **Missing CORS Configuration on Encounter & Benchmark Controllers:**  
   `EncounterController.java` and `BenchmarkController.java` lack `@CrossOrigin(origins = "http://localhost:5173")`. Add this annotation or configure a global Spring `WebMvcConfigurer`.
3. **Elapsed Time Display:**  
   `SimulationController` returns `elapsedTimeSeconds`, but `App.jsx` only shows tick count. Display formatted HH:MM:SS.

---

## 7. Security & Credentials Policy

* **Zero Plaintext Secrets:** Never hardcode passwords or commit `.env` files.
* **Environment Variable:** Use `SUPABASE_DB_PASSWORD` in your terminal or user environment.
* **Password Rotation Notice:** The historical Supabase database password was exposed in commit `fc1d9c0db5593c2342bab4c86cc9b17ca1b5b646`. The project administrator must rotate this database password in the Supabase management console.

---

## 8. Handoff Instruction for the Next Member

> [!NOTE]
> **Testing is Complete. Focus on Implementation.**  
> The backend integration, database persistence, and maneuver lifecycle have been fully verified against Supabase.  
> **Do not re-run database-writing integration tests.**  
> Your task is to implement the missing frontend UI features (Scenario selector, Maneuver planning buttons, post-maneuver state display, and benchmark view).
