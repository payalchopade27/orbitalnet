import React from 'react';
import './MetricCard.css';

function MetricCard({ icon, label, value, note, color = '' }) {
  return (
    <div className={`metric-card ${color}`}>
      <div className="metric-icon">{icon}</div>
      <div className="metric-label">{label}</div>
      <div className="metric-value">{value}</div>
      {note && <div className="metric-note">{note}</div>}
    </div>
  );
}

export default MetricCard;
