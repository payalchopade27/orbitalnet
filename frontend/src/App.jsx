import {
  Orbit,
  Satellite,
  Activity,
  AlertTriangle,
  Gauge,
  Play,
  Square,
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
import OrbitVisualization from "./components/OrbitVisualization";

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
  const [currentTick, setCurrentTick] = useState(0);
  const [isAutoRunning, setIsAutoRunning] = useState(false);
  const [isFullScreen, setIsFullScreen] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
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

  const generateScenario = async (settings = { satellites: 10, debris: 20, duration: 60 }) => {
    setIsAutoRunning(false);
    setRunId(null);
    setEncounters([]);
    setCurrentTick(0);
    setMessage("");
    setSimulationStatus("Ready to run");

    try {
      const response = await fetch(api("/api/scenarios/generate"), {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          satellites: Number(settings.satellites ?? 10),
          debris: Number(settings.debris ?? 20),
          duration: Number(settings.duration ?? 60),
        }),
      });
      if (!response.ok) {
        throw new Error("Failed to generate scenario");
      }
      const data = await response.json();
      console.log("Backend response:", data);
      setScenario(data);
      return data;
    } catch (error) {
      console.error("Error:", error);
      setMessage("Could not generate the scenario. Check the backend.");
      setSimulationStatus("Failed");
      return null;
    }
  };

  async function runSimulation(param = null, autoRun = true) {
    let sc = (param && param.scenarioId) ? param : scenario;
    if (!sc || !sc.scenarioId) {
      const config = (param && param.satellites)
        ? { satellites: param.satellites, debris: param.debris, duration: param.duration }
        : { satellites: 10, debris: 20, duration: 60 };
      sc = await generateScenario(config);
      if (!sc) return null;
    }
    try {
      setSimulationStatus("Starting...");
      const response = await fetch(api(`/api/simulations/${sc.scenarioId}/run`), {
        method: "POST",
      });
      if (!response.ok) {
        throw new Error("Failed to start simulation");
      }
      const data = await response.json();
      setSimulationStatus(data.status);
      setRunId(data.runId);
      setCurrentTick(0);
      setEncounters([]);
      setTrackedObjects([]);
      const stateResponse = await fetch(api(`/api/simulations/${data.runId}/state`));
      if (!stateResponse.ok) {
        throw new Error("Failed to fetch tracked objects");
      }
      const stateData = await stateResponse.json();
      setTrackedObjects(stateData.objects || []);
      setMessage(data.message || "Simulation started successfully.");
      setIsAutoRunning(autoRun);
      console.log("Tracked objects:", stateData.objects);
      console.log("Simulation Run ID:", data.runId);
      return data.runId;
    } catch (error) {
      console.error("Simulation error:", error);
      setSimulationStatus("Failed");
      setMessage(error.message || "Could not start simulation. Check the backend.");
      setIsAutoRunning(false);
      return null;
    }
  }

  async function stepSimulation(targetRunId = null) {
    let currentId = targetRunId || runId;
    if (!currentId || simulationStatus === "COMPLETED" || simulationStatus === "Failed") {
      currentId = await runSimulation(scenario, false);
      if (!currentId) return;
    }
    try {
      const response = await fetch(api(`/api/simulations/${currentId}/step`), {
        method: "POST",
      });
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

  function handleStopSimulation() {
    setIsAutoRunning(false);
    setSimulationStatus("STOPPED");
    setMessage("Simulation stopped.");
  }

  async function handleToggleAutoRun() {
    if (isAutoRunning) {
      setIsAutoRunning(false);
      setSimulationStatus("PAUSED");
      setMessage("Simulation paused.");
      return;
    }
    if (isProcessing) return;
    setIsProcessing(true);
    try {
      if (!runId || simulationStatus === "COMPLETED" || simulationStatus === "Failed") {
        await runSimulation(scenario, true);
      } else {
        setSimulationStatus("RUNNING");
        setIsAutoRunning(true);
      }
    } finally {
      setIsProcessing(false);
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

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === "Escape" && isFullScreen) {
        setIsFullScreen(false);
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isFullScreen]);


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

        <ScenarioControls
          onGenerate={generateScenario}
          onRun={(cfg) => runSimulation(cfg, true)}
        />

        {message && (
          <div className="panel scenario-message" role="status">
            <strong>{simulationStatus}</strong>
            <p>{message}</p>
          </div>
        )}

        {scenario && (
          <div className="panel scenario-result">
            <div className="panel-heading">
              <div>
                <h3>Scenario Generated Successfully</h3>
                <p>Configuration overview</p>
              </div>
              <span className={`status-pill ${scenario.status === "READY" ? "" : "warning"}`}>
                {scenario.status}
              </span>
            </div>
            <div className="scenario-result-grid">
              <div className="result-card">
                <span className="result-label">Status</span>
                <span className="result-value">{scenario.status}</span>
              </div>
              <div className="result-card">
                <span className="result-label">Satellites</span>
                <span className="result-value">{scenario.satellites}</span>
              </div>
              <div className="result-card">
                <span className="result-label">Debris Objects</span>
                <span className="result-value">{scenario.debris}</span>
              </div>
              <div className="result-card">
                <span className="result-label">Duration</span>
                <span className="result-value">{scenario.duration} min</span>
              </div>
            </div>
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

            <div className={`orbit-3d-wrapper ${isFullScreen ? "fullscreen" : ""}`}>
              <OrbitVisualization
                trackedObjects={trackedObjects}
                selectedObject={selectedObject}
                onSelectObject={setSelectedObject}
                selectedEncounter={selectedEncounter}
                isFullScreen={isFullScreen}
                onToggleFullScreen={() => setIsFullScreen((prev) => !prev)}
              />
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
                  onClick={handleStopSimulation}
                  disabled={isProcessing}
                  title="Stop simulation"
                >
                  <Square size={13} fill="currentColor" /> Stop Simulation
                </button>
                <button
                  className="primary-button"
                  onClick={handleToggleAutoRun}
                  disabled={isProcessing}
                  title={isAutoRunning ? "Pause simulation" : "Start continuous simulation"}
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
              <div className="selected-object-body">
                <div className="object-meta-grid">
                  <div className="object-meta-item full-width">
                    <span className="meta-label">Object ID</span>
                    <span className="meta-value mono">{selectedObject.id}</span>
                  </div>
                  <div className="object-meta-item">
                    <span className="meta-label">Type</span>
                    <span className="meta-value">{selectedObject.type === "DEBRIS" ? "Space debris" : "Active satellite"}</span>
                  </div>
                  <div className="object-meta-item">
                    <span className="meta-label">Status</span>
                    <span className="meta-value">{selectedObject.status}</span>
                  </div>
                </div>
                <div className="vector-group">
                  <h4 className="vector-heading">Position</h4>
                  <div className="coords-grid">
                    <div className="coord-item">
                      <span className="axis-tag">X</span>
                      <span className="coord-val">{Number(selectedObject.position?.x ?? 0).toFixed(2)}</span>
                    </div>
                    <div className="coord-item">
                      <span className="axis-tag">Y</span>
                      <span className="coord-val">{Number(selectedObject.position?.y ?? 0).toFixed(2)}</span>
                    </div>
                    <div className="coord-item">
                      <span className="axis-tag">Z</span>
                      <span className="coord-val">{Number(selectedObject.position?.z ?? 0).toFixed(2)}</span>
                    </div>
                  </div>
                </div>
                <div className="vector-group">
                  <h4 className="vector-heading">Velocity</h4>
                  <div className="coords-grid">
                    <div className="coord-item">
                      <span className="axis-tag">X</span>
                      <span className="coord-val">{Number(selectedObject.velocity?.x ?? 0).toFixed(2)}</span>
                    </div>
                    <div className="coord-item">
                      <span className="axis-tag">Y</span>
                      <span className="coord-val">{Number(selectedObject.velocity?.y ?? 0).toFixed(2)}</span>
                    </div>
                    <div className="coord-item">
                      <span className="axis-tag">Z</span>
                      <span className="coord-val">{Number(selectedObject.velocity?.z ?? 0).toFixed(2)}</span>
                    </div>
                  </div>
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
          {selectedEncounter && (
            <div className="panel encounter-details-panel">
              <div className="panel-heading">
                <div>
                  <h3>Encounter Details</h3>
                  <p>Review the selected close approach</p>
                </div>
                <button
                  className="secondary-button"
                  onClick={() => setSelectedEncounter(null)}
                >
                  Close
                </button>
              </div>
              <div className="selected-object-details">
                <p><strong>Encounter ID:</strong> {selectedEncounter.encounterId}</p>
                <p><strong>Object A:</strong> {selectedEncounter.objectAId ?? "N/A"}</p>
                <p><strong>Object B:</strong> {selectedEncounter.objectBId ?? "N/A"}</p>
                <p><strong>Miss distance:</strong> {selectedEncounter.minSeparationKm !== undefined ? `${Number(selectedEncounter.minSeparationKm).toFixed(2)} km` : "N/A"}</p>
                <p><strong>Time to closest approach:</strong> {selectedEncounter.timeToClosestApproachSeconds !== undefined ? `${Number(selectedEncounter.timeToClosestApproachSeconds).toFixed(2)} seconds` : "N/A"}</p>
                <p><strong>Risk score:</strong> {Number(selectedEncounter.riskScore ?? 0)}</p>
                <p><strong>Risk level:</strong> {getRiskLevel(Number(selectedEncounter.riskScore ?? 0))}</p>
              </div>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}

export default App;
