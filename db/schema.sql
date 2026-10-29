-- ============================================================
-- OrbitalNet Database Schema
-- PostgreSQL / Supabase
-- ============================================================

-- ============================================================
-- 1. SCENARIO
-- ============================================================

CREATE TABLE IF NOT EXISTS scenario (
    scenario_id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255),
    seed BIGINT NOT NULL,
    generation_config JSONB NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'READY',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_scenario_status
        CHECK (status IN ('READY', 'FAILED'))
);


-- ============================================================
-- 2. SPACE OBJECT
-- ============================================================

CREATE TABLE IF NOT EXISTS space_object (
    object_id VARCHAR(100) PRIMARY KEY,
    scenario_id VARCHAR(100) NOT NULL,
    object_type VARCHAR(50) NOT NULL,
    name VARCHAR(255),
    source_type VARCHAR(50),

    size DOUBLE PRECISION NOT NULL,

    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',

    pos_x DOUBLE PRECISION NOT NULL,
    pos_y DOUBLE PRECISION NOT NULL,
    pos_z DOUBLE PRECISION NOT NULL,

    vel_x DOUBLE PRECISION NOT NULL,
    vel_y DOUBLE PRECISION NOT NULL,
    vel_z DOUBLE PRECISION NOT NULL,

    altitude_km DOUBLE PRECISION,
    inclination_deg DOUBLE PRECISION,
    period_seconds DOUBLE PRECISION,

    CONSTRAINT chk_object_size
        CHECK (size >= 0),

    CONSTRAINT chk_object_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DESTROYED')),

    CONSTRAINT chk_object_type
        CHECK (object_type IN ('SATELLITE', 'DEBRIS')),

    CONSTRAINT chk_source_type
        CHECK (
            source_type IS NULL
            OR source_type IN ('FRAGMENT', 'ROCKET_FRAGMENT', 'UNKNOWN')
        ),

    CONSTRAINT fk_space_object_scenario
        FOREIGN KEY (scenario_id)
        REFERENCES scenario(scenario_id)
        ON DELETE CASCADE
);


-- ============================================================
-- 3. SIMULATION RUN
-- ============================================================

CREATE TABLE IF NOT EXISTS simulation_run (
    run_id VARCHAR(100) PRIMARY KEY,

    scenario_id VARCHAR(100) NOT NULL,

    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',

    current_tick INTEGER NOT NULL DEFAULT 0,

    elapsed_time_seconds DOUBLE PRECISION NOT NULL DEFAULT 0,

    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,

    algorithm_used VARCHAR(100),

    baseline_run_id VARCHAR(100),

    maneuver_candidate_id VARCHAR(100),

    CONSTRAINT chk_current_tick
        CHECK (current_tick >= 0),

    CONSTRAINT chk_elapsed_time
        CHECK (elapsed_time_seconds >= 0),

    CONSTRAINT chk_not_self_baseline
        CHECK (
            baseline_run_id IS NULL
            OR baseline_run_id <> run_id
        ),

    CONSTRAINT chk_simulation_run_status
        CHECK (
            status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED')
        ),

    CONSTRAINT fk_simulation_run_scenario
        FOREIGN KEY (scenario_id)
        REFERENCES scenario(scenario_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_simulation_run_baseline
        FOREIGN KEY (baseline_run_id)
        REFERENCES simulation_run(run_id)
        ON DELETE RESTRICT
);


-- ============================================================
-- 4. ENCOUNTER
-- ============================================================

CREATE TABLE IF NOT EXISTS encounter (
    encounter_id VARCHAR(100) PRIMARY KEY,

    run_id VARCHAR(100) NOT NULL,

    object_a_id VARCHAR(100) NOT NULL,
    object_b_id VARCHAR(100) NOT NULL,

    min_separation_km DOUBLE PRECISION NOT NULL,

    time_to_closest_approach_seconds DOUBLE PRECISION NOT NULL,

    relative_velocity_km_per_sec DOUBLE PRECISION NOT NULL,

    detected_at_tick INTEGER NOT NULL,

    status VARCHAR(50) NOT NULL DEFAULT 'DETECTED',

    CONSTRAINT chk_different_objects
        CHECK (object_a_id <> object_b_id),

    CONSTRAINT chk_min_separation
        CHECK (min_separation_km >= 0),

    CONSTRAINT chk_time_to_ca
        CHECK (
            time_to_closest_approach_seconds >= 0
            AND time_to_closest_approach_seconds <= 3600
        ),

    CONSTRAINT chk_relative_velocity
        CHECK (relative_velocity_km_per_sec >= 0),

    CONSTRAINT chk_detected_tick
        CHECK (detected_at_tick >= 0),

    CONSTRAINT chk_encounter_status
        CHECK (
            status IN (
                'DETECTED',
                'ASSESSED',
                'MITIGATED',
                'RESOLVED'
            )
        ),

    CONSTRAINT fk_encounter_run
        FOREIGN KEY (run_id)
        REFERENCES simulation_run(run_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_encounter_object_a
        FOREIGN KEY (object_a_id)
        REFERENCES space_object(object_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_encounter_object_b
        FOREIGN KEY (object_b_id)
        REFERENCES space_object(object_id)
        ON DELETE RESTRICT
);


-- ============================================================
-- 5. RISK ASSESSMENT
-- ============================================================

CREATE TABLE IF NOT EXISTS risk_assessment (
    encounter_id VARCHAR(100) PRIMARY KEY,

    total_score INTEGER NOT NULL,

    separation_factor DOUBLE PRECISION NOT NULL,

    velocity_factor DOUBLE PRECISION NOT NULL,

    time_factor DOUBLE PRECISION NOT NULL,

    explanation TEXT NOT NULL,

    CONSTRAINT chk_total_score
        CHECK (total_score >= 0 AND total_score <= 100),

    CONSTRAINT chk_separation_factor
        CHECK (separation_factor >= 0 AND separation_factor <= 1),

    CONSTRAINT chk_velocity_factor
        CHECK (velocity_factor >= 0 AND velocity_factor <= 1),

    CONSTRAINT chk_time_factor
        CHECK (time_factor >= 0 AND time_factor <= 1),

    CONSTRAINT fk_risk_encounter
        FOREIGN KEY (encounter_id)
        REFERENCES encounter(encounter_id)
        ON DELETE CASCADE
);


-- ============================================================
-- 6. MANEUVER CANDIDATE
-- ============================================================

CREATE TABLE IF NOT EXISTS maneuver_candidate (
    maneuver_candidate_id VARCHAR(100) PRIMARY KEY,

    encounter_id VARCHAR(100) NOT NULL,

    target_object_id VARCHAR(100) NOT NULL,

    delta_v_x DOUBLE PRECISION NOT NULL,
    delta_v_y DOUBLE PRECISION NOT NULL,
    delta_v_z DOUBLE PRECISION NOT NULL,

    new_min_separation_km DOUBLE PRECISION NOT NULL,

    cost DOUBLE PRECISION NOT NULL,

    rank INTEGER NOT NULL,

    CONSTRAINT chk_new_min_separation
        CHECK (new_min_separation_km >= 0),

    CONSTRAINT chk_maneuver_cost
        CHECK (cost >= 0),

    CONSTRAINT chk_maneuver_rank
        CHECK (rank > 0),

    CONSTRAINT uq_maneuver_encounter_rank
        UNIQUE (encounter_id, rank),

    CONSTRAINT fk_maneuver_encounter
        FOREIGN KEY (encounter_id)
        REFERENCES encounter(encounter_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_maneuver_target_object
        FOREIGN KEY (target_object_id)
        REFERENCES space_object(object_id)
        ON DELETE RESTRICT
);


-- ============================================================
-- 7. COMPLETE CIRCULAR FOREIGN KEY
-- ============================================================

ALTER TABLE simulation_run
ADD CONSTRAINT fk_simulation_run_maneuver_candidate
FOREIGN KEY (maneuver_candidate_id)
REFERENCES maneuver_candidate(maneuver_candidate_id)
ON DELETE RESTRICT;


-- ============================================================
-- 8. FINAL OBJECT STATE
-- ============================================================

CREATE TABLE IF NOT EXISTS final_object_state (
    run_id VARCHAR(100) NOT NULL,

    object_id VARCHAR(100) NOT NULL,

    pos_x DOUBLE PRECISION NOT NULL,
    pos_y DOUBLE PRECISION NOT NULL,
    pos_z DOUBLE PRECISION NOT NULL,

    vel_x DOUBLE PRECISION NOT NULL,
    vel_y DOUBLE PRECISION NOT NULL,
    vel_z DOUBLE PRECISION NOT NULL,

    status VARCHAR(50) NOT NULL,

    CONSTRAINT final_object_state_pkey
        PRIMARY KEY (run_id, object_id),

    CONSTRAINT chk_final_state_status
        CHECK (
            status IN ('ACTIVE', 'INACTIVE', 'DESTROYED')
        ),

    CONSTRAINT fk_final_state_run
        FOREIGN KEY (run_id)
        REFERENCES simulation_run(run_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_final_state_object
        FOREIGN KEY (object_id)
        REFERENCES space_object(object_id)
        ON DELETE RESTRICT
);


-- ============================================================
-- 9. BENCHMARK RESULT
-- ============================================================

CREATE TABLE IF NOT EXISTS benchmark_result (
    benchmark_id VARCHAR(100) PRIMARY KEY,

    run_id VARCHAR(100),

    object_count INTEGER NOT NULL,

    naive_checks BIGINT NOT NULL,

    indexed_checks BIGINT NOT NULL,

    naive_ms DOUBLE PRECISION NOT NULL,

    indexed_ms DOUBLE PRECISION NOT NULL,

    CONSTRAINT chk_benchmark_object_count
        CHECK (object_count >= 0),

    CONSTRAINT chk_naive_checks
        CHECK (naive_checks >= 0),

    CONSTRAINT chk_indexed_checks
        CHECK (indexed_checks >= 0),

    CONSTRAINT chk_naive_ms
        CHECK (naive_ms >= 0),

    CONSTRAINT chk_indexed_ms
        CHECK (indexed_ms >= 0),

    CONSTRAINT fk_benchmark_run
        FOREIGN KEY (run_id)
        REFERENCES simulation_run(run_id)
        ON DELETE SET NULL
);


-- ============================================================
-- 10. EVENT LOG
-- ============================================================

CREATE TABLE IF NOT EXISTS event_log (
    event_id VARCHAR(100) PRIMARY KEY,

    run_id VARCHAR(100) NOT NULL,

    tick INTEGER NOT NULL,

    event_type VARCHAR(100) NOT NULL,

    message TEXT NOT NULL,

    logged_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_event_tick
        CHECK (tick >= 0),

    CONSTRAINT fk_event_log_run
        FOREIGN KEY (run_id)
        REFERENCES simulation_run(run_id)
        ON DELETE CASCADE
);


-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_space_object_scenario
    ON space_object(scenario_id);

CREATE INDEX IF NOT EXISTS idx_simulation_run_scenario
    ON simulation_run(scenario_id);

CREATE INDEX IF NOT EXISTS idx_simulation_run_baseline
    ON simulation_run(baseline_run_id);

CREATE INDEX IF NOT EXISTS idx_encounter_run
    ON encounter(run_id);

CREATE INDEX IF NOT EXISTS idx_encounter_object_a
    ON encounter(object_a_id);

CREATE INDEX IF NOT EXISTS idx_encounter_object_b
    ON encounter(object_b_id);

CREATE INDEX IF NOT EXISTS idx_encounter_status
    ON encounter(status);

CREATE INDEX IF NOT EXISTS idx_maneuver_encounter
    ON maneuver_candidate(encounter_id);

CREATE INDEX IF NOT EXISTS idx_maneuver_target_object
    ON maneuver_candidate(target_object_id);

CREATE INDEX IF NOT EXISTS idx_final_state_run
    ON final_object_state(run_id);

CREATE INDEX IF NOT EXISTS idx_benchmark_run
    ON benchmark_result(run_id);

CREATE INDEX IF NOT EXISTS idx_event_log_run_tick
    ON event_log(run_id, tick);


-- ============================================================
-- END OF SCHEMA
-- ============================================================