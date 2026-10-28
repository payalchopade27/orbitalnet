-- ============================================
-- OrbitalNet - Analytical SQL Queries
-- Member 3: Data & Software Engineering
-- ============================================


-- Query 1: Count total objects in each scenario
SELECT
    s.scenario_id,
    s.name,
    COUNT(so.object_id) AS total_objects
FROM scenario s
LEFT JOIN space_object so
    ON s.scenario_id = so.scenario_id
GROUP BY s.scenario_id, s.name;


-- Query 2: Count satellites and debris in each scenario
SELECT
    s.scenario_id,
    COUNT(CASE WHEN so.object_type = 'SATELLITE' THEN 1 END) AS satellite_count,
    COUNT(CASE WHEN so.object_type = 'DEBRIS' THEN 1 END) AS debris_count
FROM scenario s
LEFT JOIN space_object so
    ON s.scenario_id = so.scenario_id
GROUP BY s.scenario_id;


-- Query 3: Show all simulation runs with their scenarios
SELECT
    sr.run_id,
    sr.scenario_id,
    s.name AS scenario_name,
    sr.status,
    sr.current_tick,
    sr.elapsed_time_seconds,
    sr.algorithm_used
FROM simulation_run sr
JOIN scenario s
    ON sr.scenario_id = s.scenario_id
ORDER BY sr.scenario_id, sr.run_id;


-- Query 4: Show encounters along with their risk assessment
SELECT
    e.encounter_id,
    e.run_id,
    e.object_a_id,
    e.object_b_id,
    e.min_separation_km,
    e.time_to_closest_approach_seconds,
    e.relative_velocity_km_per_sec,
    e.status,
    r.total_score,
    r.explanation
FROM encounter e
LEFT JOIN risk_assessment r
    ON e.encounter_id = r.encounter_id
ORDER BY r.total_score DESC;


-- Query 5: Find the highest-risk encounter
SELECT
    e.encounter_id,
    e.run_id,
    r.total_score,
    r.explanation
FROM encounter e
JOIN risk_assessment r
    ON e.encounter_id = r.encounter_id
WHERE r.total_score = (
    SELECT MAX(total_score)
    FROM risk_assessment
);


-- Query 6: Find unresolved encounters
SELECT
    e.encounter_id,
    e.run_id,
    e.object_a_id,
    e.object_b_id,
    e.min_separation_km,
    e.status
FROM encounter e
WHERE e.status <> 'RESOLVED';


-- Query 7: Show all maneuver candidates for each encounter
SELECT
    mc.maneuver_candidate_id,
    mc.encounter_id,
    mc.target_object_id,
    mc.delta_v_x,
    mc.delta_v_y,
    mc.delta_v_z,
    mc.new_min_separation_km,
    mc.cost,
    mc.rank
FROM maneuver_candidate mc
ORDER BY mc.encounter_id, mc.rank;


-- Query 8: Find the best-ranked maneuver candidate for each encounter
SELECT
    mc.maneuver_candidate_id,
    mc.encounter_id,
    mc.target_object_id,
    mc.new_min_separation_km,
    mc.cost,
    mc.rank
FROM maneuver_candidate mc
WHERE mc.rank = (
    SELECT MIN(mc2.rank)
    FROM maneuver_candidate mc2
    WHERE mc2.encounter_id = mc.encounter_id
)
ORDER BY mc.encounter_id;

-- ============================================
-- Advanced Analytical Queries
-- ============================================


-- Query 9: Find the scenario with the highest number of objects
SELECT
    s.scenario_id,
    s.name,
    COUNT(so.object_id) AS total_objects
FROM scenario s
JOIN space_object so
    ON s.scenario_id = so.scenario_id
GROUP BY s.scenario_id, s.name
HAVING COUNT(so.object_id) = (
    SELECT MAX(object_count)
    FROM (
        SELECT COUNT(*) AS object_count
        FROM space_object
        GROUP BY scenario_id
    ) AS scenario_counts
);


-- Query 10: Find encounters having risk score greater than the average risk
SELECT
    e.encounter_id,
    e.run_id,
    r.total_score,
    r.explanation
FROM encounter e
JOIN risk_assessment r
    ON e.encounter_id = r.encounter_id
WHERE r.total_score > (
    SELECT AVG(total_score)
    FROM risk_assessment
)
ORDER BY r.total_score DESC;


-- Query 11: Find the most expensive maneuver candidate
SELECT
    mc.maneuver_candidate_id,
    mc.encounter_id,
    mc.target_object_id,
    mc.cost,
    mc.rank
FROM maneuver_candidate mc
WHERE mc.cost = (
    SELECT MAX(cost)
    FROM maneuver_candidate
);


-- Query 12: Find the maneuver candidate giving the maximum
-- improvement in minimum separation
SELECT
    mc.maneuver_candidate_id,
    mc.encounter_id,
    mc.target_object_id,
    mc.new_min_separation_km,
    mc.cost,
    mc.rank
FROM maneuver_candidate mc
WHERE mc.new_min_separation_km = (
    SELECT MAX(new_min_separation_km)
    FROM maneuver_candidate
);


-- Query 13: Show risk category for every encounter
SELECT
    e.encounter_id,
    r.total_score,
    CASE
        WHEN r.total_score BETWEEN 0 AND 24 THEN 'Low'
        WHEN r.total_score BETWEEN 25 AND 49 THEN 'Moderate'
        WHEN r.total_score BETWEEN 50 AND 74 THEN 'High'
        WHEN r.total_score BETWEEN 75 AND 100 THEN 'Critical'
    END AS risk_level
FROM encounter e
JOIN risk_assessment r
    ON e.encounter_id = r.encounter_id
ORDER BY r.total_score DESC;


-- Query 14: Find the average risk score for each simulation run
SELECT
    e.run_id,
    AVG(r.total_score) AS average_risk_score
FROM encounter e
JOIN risk_assessment r
    ON e.encounter_id = r.encounter_id
GROUP BY e.run_id
ORDER BY average_risk_score DESC;


-- Query 15: Find simulation runs that contain at least one
-- high or critical risk encounter
SELECT DISTINCT
    e.run_id
FROM encounter e
JOIN risk_assessment r
    ON e.encounter_id = r.encounter_id
WHERE r.total_score >= 50;