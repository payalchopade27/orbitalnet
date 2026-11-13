import {
  Orbit,
  Satellite,
  Activity,
  AlertTriangle,
  Gauge,
  Play,
  Radar,
  ShieldCheck,
  CircleDot,
} from "lucide-react";
import "./App.css";

// Configurable backend API base URL
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
// Helper to build full API URLs
const api = (path) => `${API_BASE_URL}${path}`;

import { useState, useEffect, useRef } from "react";
import ScenarioControls from "./components/ScenarioControls";
import MetricCard from "./components/MetricCard"; // assuming this component exists

function formatTime(seconds) {
  const totalSeconds = Math.max(0, Math.floor(seconds || 0));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const remainingSeconds = totalSeconds % 60;
  return [hours, minutes, remainingSeconds]
    .map((value) => String(value).padStart(2, "0"))
    .join(":");
}

function getRiskLevel(score) {
  if (score >= 70) return "High";
  if (score >= 40) return "Medium";
  return "Low";
}

function App() {
  const [scenario, setScenario] = useState(null);
  const [simulationStatus, setSimulationStatus] = useState("Ready to run");
  const [message, setMessage] = useState("");
  const [runId, setRunId] = useState(null);
  const [encounters, setEncounters] = useState([]);
  const [riskFilter, setRiskFilter] = useState("ALL");
  const [riskSort, setRiskSort] = useState("DEFAULT");
  const [trackedObjects, setTrackedObjects] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [objectTypeFilter, setObjectTypeFilter] = useState("ALL");
  const [selectedObject, setSelectedObject] = useState(null);
  const [selectedEncounter, setSelectedEncounter] = useState(null);
  const [maneuverCandidates, setManeuverCandidates] = useState([]);
  const [maneuverLoading, setManeuverLoading] = useState(false);
  const [maneuverMessage, setManeuverMessage] = useState("");
  const [currentTick, setCurrentTick] = useState(0);
  const [isAutoRunning, setIsAutoRunning] = useState(false);
  const stepInProgress = useRef(false);

  const filteredEncounters = encounters
    .filter((encounter) => {
      if (riskFilter === "ALL") return true;
      const riskLevel = getRiskLevel(Number(encounter.riskScore));
      return riskLevel.toUpperCase() === riskFilter;
    })
    .sort((a, b) => {
      if (riskSort === "HIGH_FIRST") {
        return Number(b.riskScore) - Number(a.riskScore);
      }
      if (riskSort === "LOW_FIRST") {
        return Number(a.riskScore) - Number(b.riskScore);
      }
      return 0;
    });

  const filteredObjects = trackedObjects.filter((object) => {
    const matchesSearch = object.id.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesType = objectTypeFilter === "ALL" || object.type === objectTypeFilter;
    return matchesSearch && matchesType;
  });

  const generateScenario = async (settings) => {
    setIsAutoRunning(false);
    setRunId(null);
    setEncounters([]);
    setCurrentTick(0);
    setMessage("");
    setSimulationStatus("Ready to run");

    try {
      const response = await fetch(
        "http://localhost:8080/api/scenarios/generate",
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            satellites: settings.satellites,
            debris: settings.debris,
            duration: settings.duration,
          }),
        }
      );
      if (!response.ok) {
        throw new Error("Failed to generate scenario");
      }
      const data = await response.json();
      console.log("Backend response:", data);
      setScenario(data);
    } catch (error) {
      console.error("Error:", error);
      setMessage("Could not generate the scenario. Check the backend.");
      setSimulationStatus("Failed");
    }
  };

  async function runSimulation() {
    if (!scenario) {
      setMessage("Please generate a scenario first.");
      return;
    }
    try {
      setSimulationStatus("Starting...");
      const response = await fetch(
        `http://localhost:8080/api/simulations/${scenario.scenarioId}/run`,
        {
          method: "POST",
        }
      );
      if (!response.ok) {
        throw new Error("Failed to start simulation");
      }
      const data = await response.json();
      setSimulationStatus(data.status);
      setIsAutoRunning(true);
      setRunId(data.runId);
      setCurrentTick(0);
      setEncounters([]);
      setTrackedObjects([]);
      const stateResponse = await fetch(
        `http://localhost:8080/api/simulations/${data.runId}/state`
      );
      if (!stateResponse.ok) {
        throw new Error("Failed to fetch tracked objects");
      }
      const stateData = await stateResponse.json();
      setTrackedObjects(stateData.objects || []);
      setMessage(data.message || "Simulation started successfully.");
      console.log("Tracked objects:", stateData.objects);
      console.log("Simulation Run ID:", data.runId);
    } catch (error) {
      console.error("Simulation error:", error);
      setSimulationStatus("Failed");
      setMessage(error.message || "Could not start simulation. Check the backend.");
      setIsAutoRunning(false);
    }
  }

  async function stepSimulation() {
    if (!runId) {
      setMessage("Please generate a scenario and run the simulation first.");
      return;
    }
    try {
      const response = await fetch(
        `http://localhost:8080/api/simulations/${runId}/step`,
        {
          method: "POST",
        }
      );
      if (!response.ok) {
        throw new Error("Failed to advance simulation");
      }
      const data = await response.json();
      setTrackedObjects(data.objects || []);
      setCurrentTick(data.currentTick);
      setSimulationStatus(data.status);
      setEncounters(data.encounters || []);
      if (data.status === "COMPLETED") {
        setIsAutoRunning(false);
      }
      setMessage(`Simulation advanced to tick ${data.currentTick}.`);
      console.log("Tracked objects:", data.objects);
      console.log("Encounters received:", data.encounters);
    } catch (error) {
      console.error("Step simulation error:", error);
      setSimulationStatus("Failed");
      setMessage(error.message || "Could not advance simulation.");
      setIsAutoRunning(false);
    }
  }

  useEffect(() => {
    if (!isAutoRunning || !runId || simulationStatus === "COMPLETED" || simulationStatus === "Failed") {
      return;
    }
    const timeoutId = setTimeout(async () => {
      if (stepInProgress.current) return;
      stepInProgress.current = true;
      try {
        await stepSimulation();
      } finally {
        stepInProgress.current = false;
      }
    }, 1000);
    return () => clearTimeout(timeoutId);
  }, [isAutoRunning, runId, simulationStatus, currentTick]);

  // Fetch saved maneuver candidates when an encounter is selected
  useEffect(() => {
    let ignored = false;
    if (!selectedEncounter) {
      setManeuverCandidates([]);
      setManeuverMessage("");
      setManeuverLoading(false);
      return;
    }
    setManeuverLoading(true);
    setManeuverMessage("");
    fetch(api(`/api/encounters/${selectedEncounter.encounterId}/maneuvers`))
      .then((response) => {
        if (!response.ok) {
          return response.json().then((err) => {
            throw new Error(err?.error || "Failed to load maneuver candidates");
          });
        }
        return response.json();
      })
      .then((data) => {
        if (ignored) return;
        setManeuverCandidates(data);
        if (Array.isArray(data) && data.length === 0) {
          setManeuverMessage("No saved maneuver candidates for this encounter.");
        }
      })
      .catch((err) => {
        if (ignored) return;
        console.error(err);
        setManeuverMessage(err.message || "Failed to load maneuver candidates.");
      })
      .finally(() => {
        if (!ignored) setManeuverLoading(false);
      });
    return () => {
      ignored = true;
    };
  }, [selectedEncounter]);

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-icon">
            <Orbit size={25} />
          </div>
          <div>
            <h2>OrbitalNet</h2>
            <span>SPACE TRAFFIC SYSTEM</span>
          </div>
        </div>
        <div className="nav-label">WORKSPACE</div>
        <nav className="navigation">
          <a className="nav-item active" href="#overview">
            <Gauge size={18} /> Overview
          </a>
          <a className="nav-item" href="#objects">
            <Satellite size={18} /> Space Objects
          </a>
          <a className="nav-item" href="#encounters">
            <AlertTriangle size={18} /> Encounters<span className="nav-count">{encounters.length}</span>
          </a>
          <a className="nav-item" href="#simulation">
            <Activity size={18} /> Simulation
          </a>
        </nav>
        <div className="sidebar-bottom">
          <div className="connection">
            <span className="live-dot" />
            <div>
              <strong>Frontend online</strong>
              <small>Backend integration</small>
            </div>
          </div>
          <div className="version">ORBITALNET · v0.1.0</div>
        </div>
      </aside>

      <main className="main-content" id="overview">
        <header className="topbar">
          <div>
            <div className="breadcrumb">Workspace / Overview</div>
            <h1>Mission Control</h1>
            <p className="subtitle">Monitor orbital objects and potential collision risks.</p>
          </div>
          <div className="system-status">
            <span className="live-dot" /> DEMO ENVIRONMENT
          </div>
        </header>

        <section className="welcome-banner">
          <div className="welcome-copy">
            <div className="eyebrow">ORBITAL SITUATIONAL AWARENESS</div>
            <h2>Space traffic, under control.</h2>
            <p>Track satellites, detect close approaches, and review potential avoidance actions from one dashboard.</p>
            <a className="primary-button" href="#simulation">
              <Play size={16} fill="currentColor" /> View simulation
            </a>
          </div>
          <div className="orbit-art" aria-hidden="true">
            <div className="orbit-ring ring-one" />
            <div className="orbit-ring ring-two" />
            <div className="planet">
              <div className="planet-shade" />
            </div>
            <div className="orbit-satellite satellite-one">
              <Satellite size={23} />
            </div>
            <div className="orbit-satellite satellite-two">
              <CircleDot size={15} />
            </div>
          </div>
        </section>

        <section className="metrics-grid" aria-label="Simulation summary">
          <MetricCard
            icon={<Satellite size={20} />}
            label="Space objects"
            value={scenario ? Number(scenario.satellites) + Number(scenario.debris) : 0}
            note={scenario ? `${scenario.satellites} satellites · ${scenario.debris} debris` : "Generate a scenario to begin"}
            color="blue"
          />
          <MetricCard
            icon={<Radar size={20} />}
            label="Close approaches"
            value={encounters.length}
            note="Detected in current tick"
            color="purple"
          />
          <MetricCard
            icon={<AlertTriangle size={20} />}
            label="High-risk events"
            value={encounters.filter((e) => Number(e.riskScore) >= 70).length}
            note="Risk score ≥ 70"
            color="orange"
          />
          <MetricCard
            icon={<ShieldCheck size={20} />}
            label="System status"
            value={simulationStatus}
            note={`Simulation tick: ${currentTick}`}
            color="green"
          />
        </section>

        <ScenarioControls onGenerate={generateScenario} onRun={runSimulation} />

        {message && (
          <div className="panel scenario-message" role="status">
            <strong>{simulationStatus}</strong>
            <p>{message}</p>
          </div>
        )}

        {scenario && (
          <div className="panel scenario-result">
            <h3>Scenario Generated Successfully</h3>
            <p>Status: {scenario.status}</p>
            <p>Satellites: {scenario.satellites}</p>
            <p>Debris: {scenario.debris}</p>
            <p>Duration: {scenario.duration} minutes</p>
          </div>
        )}

        <section className="content-grid">
          <div className="panel orbital-panel" id="simulation">
            <div className="panel-heading">
              <div>
                <h3>Orbital visualization</h3>
                <p>Illustrative view · Not real orbital positions</p>
              </div>
              <span className="tag">DEMO</span>
            </div>

            <div className="space-view">
              <div className="stars">
                {Array.from({ length: 35 }, (_, i) => (
                  <span
                    key={i}
                    style={{
                      left: `${(i * 37 + 11) % 97}%`,
                      top: `${(i * 53 + 7) % 91}%`,
                      opacity: 0.3 + ((i * 7) % 7) / 10,
                    }}
                  />
                ))}
              </div>
              <div className="space-orbit orbit-a" />
              <div className="space-orbit orbit-b" />
              <div className="space-orbit orbit-c" />
              <div className="earth">
                <div className="earth-glow" />
              </div>

              {trackedObjects.map((object) => {
                const x = Number(object.position?.x ?? 0);
                const y = Number(object.position?.y ?? 0);
                const left = 5 + ((x + 10000) / 20000) * 90;
                const top = 95 - ((y + 10000) / 20000) * 90;
                const safeLeft = Math.max(5, Math.min(95, left));
                const safeTop = Math.max(5, Math.min(95, top));
                const isDebris = object.type === "DEBRIS";
                return (
                  <div
                    key={object.id}
                    className={`object-point ${isDebris ? "debris-point" : "satellite-point"} ${selectedObject?.id === object.id ? "selected-point" : ""}`}
                    style={{ left: `${safeLeft}%`, top: `${safeTop}%` }}
                    title={`${object.id} (${object.type})`}
                    onClick={() => setSelectedObject(object)}
                    role="button"
                    tabIndex={0}
                    aria-label={`Select ${object.id}`}
                  />
                );
              })}

              <div className="orbit-legend">
                <span><i className="legend-blue" /> Satellites</span>
                <span><i className="legend-orange" /> Debris</span>
              </div>
            </div>

            <div className="simulation-footer">
              <div>
                <small>SIMULATION STATUS</small>
                <strong>
                  <span className="live-dot" /> {simulationStatus}
                </strong>
                <small>Current tick: {currentTick}</small>
              </div>
              <div style={{ display: "flex", gap: "10px", flexWrap: "wrap" }}>
                <button
                  className="secondary-button"
                  onClick={stepSimulation}
                  disabled={
                    !runId ||
                    simulationStatus === "COMPLETED" ||
                    simulationStatus === "Failed" ||
                    isAutoRunning
                  }
                >
                  <Play size={15} /> Step Simulation
                </button>
                <button
                  className="primary-button"
                  onClick={() => setIsAutoRunning((prev) => !prev)}
                  disabled={
                    !runId ||
                    simulationStatus === "COMPLETED" ||
                    simulationStatus === "Failed"
                  }
                >
                  {isAutoRunning ? "Pause Auto Run" : "Start Auto Run"}
                </button>
              </div>
            </div>
          </div>

          {selectedObject && (
            <div className="panel selected-object-panel">
              <div className="panel-heading">
                <div>
                  <h3>Selected Object</h3>
                  <p>Details from the current simulation</p>
                </div>
                <button className="secondary-button" onClick={() => setSelectedObject(null)}>
                  Close
                </button>
              </div>
              <div className="panel-body">
                <div className="selected-object-details">
                  <p><strong>ID:</strong> {selectedObject.id}</p>
                  <p><strong>Type:</strong> {selectedObject.type === "DEBRIS" ? "Space debris" : "Active satellite"}</p>
                  <p><strong>Status:</strong> {selectedObject.status}</p>
                  <h4>Position</h4>
                  <p>X: {Number(selectedObject.position?.x ?? 0).toFixed(2)}</p>
                  <p>Y: {Number(selectedObject.position?.y ?? 0).toFixed(2)}</p>
                  <p>Z: {Number(selectedObject.position?.z ?? 0).toFixed(2)}</p>
                  <h4>Velocity</h4>
                  <p>X: {Number(selectedObject.velocity?.x ?? 0).toFixed(2)}</p>
                  <p>Y: {Number(selectedObject.velocity?.y ?? 0).toFixed(2)}</p>
                  <p>Z: {Number(selectedObject.velocity?.z ?? 0).toFixed(2)}</p>
                </div>
              </div>
            </div>
          )}

          <div className="panel objects-panel" id="objects">
            <div className="panel-heading">
              <div>
                <h3>Tracked objects ({trackedObjects.length})</h3>
                <p>
                  {trackedObjects.filter((o) => o.type === "SATELLITE").length} satellites
                  {` · `}
                  {trackedObjects.filter((o) => o.type === "DEBRIS").length} debris
                </p>
              </div>
              <Satellite size={19} className="muted-icon" />
            </div>
            <div className="object-filters">
              <input
                type="text"
                placeholder="Search by object ID (e.g. SAT-1)"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                aria-label="Search tracked objects by ID"
              />
              <select
                value={objectTypeFilter}
                onChange={(e) => setObjectTypeFilter(e.target.value)}
                aria-label="Filter tracked objects by type"
              >
                <option value="ALL">All objects</option>
                <option value="SATELLITE">Satellites</option>
                <option value="DEBRIS">Debris</option>
              </select>
            </div>
            <div className="object-list">
              {trackedObjects.length === 0 ? (
                <p className="empty-state">No tracked objects available. Run a simulation first.</p>
              ) : filteredObjects.length === 0 ? (
                <p className="empty-state">No objects match your search or selected filter.</p>
              ) : (
                filteredObjects.map((object) => (
                  <div
                    key={object.id}
                    className={`object-row ${selectedObject?.id === object.id ? "selected-object-row" : ""}`}
                    onClick={() => setSelectedObject(object)}
                  >
                    <div className="object-icon">
                      {object.type === "DEBRIS" ? <CircleDot size={18} /> : <Satellite size={18} />}
                    </div>
                    <div className="object-info">
                      <strong>{object.id}</strong>
                      <small>{object.type === "DEBRIS" ? "Space debris" : "Active satellite"}</small>
                      <small>
                        Position: ({Number(object.position?.x ?? 0).toFixed(1)}, {Number(object.position?.y ?? 0).toFixed(1)}, {Number(object.position?.z ?? 0).toFixed(1)})
                      </small>
                    </div>
                    <span className={`status-pill ${object.status !== "ACTIVE" ? "warning" : ""}`}>
                      {object.status === "ACTIVE" ? "Tracking" : object.status}
                    </span>
                  </div>
                ))
              )}
            </div>
            <div className="panel-note">
              <Activity size={16} />
              <span>Sample data only. Live tracking is not connected.</span>
            </div>
          </div>
        </section>

        <section className="panel encounters-panel" id="encounters">
          <div className="panel-heading">
            <div>
              <h3>Close approach encounters</h3>
              <p>Encounters detected during the current simulation tick</p>
            </div>
            <span className="tag">
              {encounters.length} RECORDS
            </span>
          </div>
          <div className="risk-filters">
            <label htmlFor="riskFilter">Filter by risk:</label>
            <select id="riskFilter" value={riskFilter} onChange={(e) => setRiskFilter(e.target.value)}>
              <option value="ALL">All Risks</option>
              <option value="HIGH">High Risk</option>
              <option value="MEDIUM">Medium Risk</option>
              <option value="LOW">Low Risk</option>
            </select>
            <label htmlFor="riskSort">Sort by score:</label>
            <select id="riskSort" value={riskSort} onChange={(e) => setRiskSort(e.target.value)}>
              <option value="DEFAULT">Default Order</option>
              <option value="HIGH_FIRST">Highest Risk First</option>
              <option value="LOW_FIRST">Lowest Risk First</option>
            </select>
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Encounter ID</th>
                  <th>Object A</th>
                  <th>Object B</th>
                  <th>Miss distance (km)</th>
                  <th>Time to closest approach (s)</th>
                  <th>Risk score</th>
                  <th>Risk level</th>
                </tr>
              </thead>
              <tbody>
                {filteredEncounters.map((enc) => (
                  <tr key={enc.encounterId} onClick={() => setSelectedEncounter(enc)} className={selectedEncounter?.encounterId === enc.encounterId ? 'selected-row' : ''}>
                    <td>{enc.encounterId}</td>
                    <td>{enc.objectAId ?? 'N/A'}</td>
                    <td>{enc.objectBId ?? 'N/A'}</td>
                    <td>{enc.minSeparationKm !== undefined ? enc.minSeparationKm.toFixed(2) : 'N/A'}</td>
                    <td>{enc.timeToClosestApproachSeconds !== undefined ? enc.timeToClosestApproachSeconds.toFixed(2) : 'N/A'}</td>
                    <td>{enc.riskScore}</td>
                    <td>{getRiskLevel(enc.riskScore)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {selectedEncounter && maneuverLoading && <p>Loading maneuvers...</p>}
          {selectedEncounter && maneuverMessage && <p>{maneuverMessage}</p>}
          {selectedEncounter && maneuverCandidates.length > 0 && (
            <div className="maneuver-candidates">
              {/* Render maneuver candidates as needed */}
            </div>
          )}
        </section>
      </main>
    </div>
  );
}

export default App;
