
import { useState } from "react";
import { Rocket, Play, RotateCcw } from "lucide-react";

export default function ScenarioControls({ onGenerate, onRun }) {
  const [satellites, setSatellites] = useState(10);
  const [debris, setDebris] = useState(20);
  const [duration, setDuration] = useState(60);

  function handleGenerate() {
    onGenerate({
      satellites: Number(satellites),
      debris: Number(debris),
      duration: Number(duration),
    });
  }

  return (
    <section className="panel scenario-controls">
      <div className="panel-heading">
        <div>
          <h3>Scenario configuration</h3>
          <p>Configure a demonstration orbital scenario</p>
        </div>
        <Rocket size={20} />
      </div>

      <div className="scenario-form">
        <label>
          Number of satellites
          <input
            type="number"
            min="1"
            max="100"
            value={satellites}
            onChange={(e) => setSatellites(e.target.value)}
          />
        </label>

        <label>
          Number of debris objects
          <input
            type="number"
            min="0"
            max="500"
            value={debris}
            onChange={(e) => setDebris(e.target.value)}
          />
        </label>

        <label>
          Duration (minutes)
          <input
            type="number"
            min="1"
            max="1440"
            value={duration}
            onChange={(e) => setDuration(e.target.value)}
          />
        </label>

        <div className="scenario-actions">
          <button className="primary-button" onClick={handleGenerate}>
            <Rocket size={16} />
            Generate scenario
          </button>

          <button className="secondary-button" onClick={onRun}>
            <Play size={15} />
            Run simulation
          </button>

          <button
            className="secondary-button"
            onClick={() => {
              setSatellites(10);
              setDebris(20);
              setDuration(60);
            }}
          >
            <RotateCcw size={15} />
            Reset form
          </button>
        </div>
      </div>
    </section>
  );
}
