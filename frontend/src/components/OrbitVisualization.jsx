import React, { useEffect, useMemo, useRef, useState } from "react";
import * as THREE from "three";
import { Canvas, useFrame, useThree } from "@react-three/fiber";
import { OrbitControls, Stars, Line } from "@react-three/drei";
import { Maximize2, Minimize2, RotateCcw, Satellite } from "lucide-react";
import { makeEarthTexture } from "./earthTexture";
import { positionStore } from "./positionStore";

const EARTH_R = 1;

const VERTEX_SHADER = /* glsl */ `
  varying vec3 vNormal;
  void main() {
    vNormal = normalize(normalMatrix * normal);
    gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
  }
`;

const HALO_FRAGMENT = /* glsl */ `
  varying vec3 vNormal;
  void main() {
    float intensity = pow(0.65 - dot(vNormal, vec3(0.0, 0.0, 1.0)), 3.5);
    gl_FragColor = vec4(0.25, 0.55, 1.0, 1.0) * intensity;
  }
`;

const RIM_FRAGMENT = /* glsl */ `
  varying vec3 vNormal;
  void main() {
    float intensity = pow(0.75 - dot(vNormal, vec3(0.0, 0.0, 1.0)), 2.0);
    gl_FragColor = vec4(0.35, 0.65, 1.0, 1.0) * intensity * 0.55;
  }
`;

function Earth() {
  const texture = useMemo(() => makeEarthTexture(), []);
  const earthRef = useRef(null);

  useEffect(() => () => texture.dispose(), [texture]);

  useFrame((_, dt) => {
    if (earthRef.current) earthRef.current.rotation.y += dt * 0.005;
  });

  const haloMaterial = useMemo(
    () =>
      new THREE.ShaderMaterial({
        vertexShader: VERTEX_SHADER,
        fragmentShader: HALO_FRAGMENT,
        side: THREE.BackSide,
        transparent: true,
        blending: THREE.AdditiveBlending,
        depthWrite: false,
      }),
    []
  );

  const rimMaterial = useMemo(
    () =>
      new THREE.ShaderMaterial({
        vertexShader: VERTEX_SHADER,
        fragmentShader: RIM_FRAGMENT,
        side: THREE.FrontSide,
        transparent: true,
        blending: THREE.AdditiveBlending,
        depthWrite: false,
      }),
    []
  );

  useEffect(
    () => () => {
      haloMaterial.dispose();
      rimMaterial.dispose();
    },
    [haloMaterial, rimMaterial]
  );

  return (
    <group>
      <mesh ref={earthRef}>
        <sphereGeometry args={[EARTH_R, 64, 64]} />
        <meshPhongMaterial map={texture} shininess={18} specular={new THREE.Color("#1a3a5c")} />
      </mesh>
      {/* Atmospheric halo glow */}
      <mesh scale={1.22} material={haloMaterial}>
        <sphereGeometry args={[EARTH_R, 64, 64]} />
      </mesh>
      {/* Inner atmosphere rim */}
      <mesh scale={1.02} material={rimMaterial}>
        <sphereGeometry args={[EARTH_R, 64, 64]} />
      </mesh>
    </group>
  );
}

function circlePoints(radius, inclinationDeg, rotationDeg, segments = 160) {
  const pts = [];
  const incl = (inclinationDeg * Math.PI) / 180;
  const rot = (rotationDeg * Math.PI) / 180;
  const cosI = Math.cos(incl);
  const sinI = Math.sin(incl);
  const cosR = Math.cos(rot);
  const sinR = Math.sin(rot);
  for (let i = 0; i <= segments; i++) {
    const a = (i / segments) * Math.PI * 2;
    const x = Math.cos(a) * radius;
    const z = Math.sin(a) * radius;
    const y1 = -z * sinI;
    const z1 = z * cosI;
    const x2 = x * cosR + z1 * sinR;
    const z2 = -x * sinR + z1 * cosR;
    pts.push([x2, y1, z2]);
  }
  return pts;
}

const RINGS = [
  { radius: 1.08, inclination: 97, rotation: 20, color: "#38bdf8", opacity: 0.28 },
  { radius: 1.14, inclination: 55, rotation: 75, color: "#818cf8", opacity: 0.22 },
  { radius: 1.20, inclination: 28, rotation: 140, color: "#38bdf8", opacity: 0.18 },
  { radius: 1.26, inclination: 82, rotation: 300, color: "#f59e0b", opacity: 0.18 },
  { radius: 1.32, inclination: 8, rotation: 200, color: "#64748b", opacity: 0.16 },
];

function OrbitRings() {
  return (
    <group>
      {RINGS.map((r, i) => (
        <Line
          key={i}
          points={circlePoints(r.radius, r.inclination, r.rotation)}
          color={r.color}
          transparent
          opacity={r.opacity}
          lineWidth={1}
        />
      ))}
    </group>
  );
}

const TYPE_STYLE = {
  SATELLITE: { color: "#7dd3fc", emissive: "#0284c7" },
  DEBRIS: { color: "#fdba74", emissive: "#ea580c" },
  ROCKET_BODY: { color: "#c4b5fd", emissive: "#7c3aed" },
};

function mapPositionToScene(p, index = 0) {
  const x = Number(p?.x ?? 0);
  const y = Number(p?.y ?? 0);
  const z = Number(p?.z ?? 0);
  const dist = Math.sqrt(x * x + y * y + z * z);

  // If real-world km coordinates (LEO radius >= 5000 km)
  if (dist >= 5000) {
    return [x / 6371, y / 6371, z / 6371];
  }

  // If relative simulation coordinates (e.g. range [-100, 100] km)
  // Map simulation vectors onto real orbital altitude shells (1.08 - 1.32 Earth radii)
  const ringAltitudes = [1.08, 1.14, 1.20, 1.26, 1.32];
  const targetRadius = ringAltitudes[index % ringAltitudes.length];

  if (dist > 0.001) {
    const scale = targetRadius / dist;
    return [x * scale, y * scale, z * scale];
  }

  // Fallback initial phase distribution around equator
  const angle = (index * 2 * Math.PI) / 10;
  return [Math.cos(angle) * targetRadius, 0, Math.sin(angle) * targetRadius];
}

function ObjectMesh({ obj, selected, highlighted }) {
  const ref = useRef(null);
  const typeKey = obj.type === "DEBRIS" ? "DEBRIS" : obj.type === "ROCKET_BODY" ? "ROCKET_BODY" : "SATELLITE";
  const style = TYPE_STYLE[typeKey] || TYPE_STYLE.SATELLITE;

  useFrame(({ clock }) => {
    const g = ref.current;
    if (!g) return;
    const pulse = highlighted ? 1 + Math.sin(clock.elapsedTime * 6) * 0.25 : 1;
    g.scale.setScalar((selected ? 1.7 : 1) * pulse);
    g.rotation.y += 0.008;
  });

  const emissiveIntensity = selected || highlighted ? 1.7 : 0.9;

  return (
    <group ref={ref}>
      {typeKey === "SATELLITE" && (
        <>
          <mesh>
            <boxGeometry args={[0.016, 0.016, 0.016]} />
            <meshStandardMaterial
              color={style.color}
              emissive={style.emissive}
              emissiveIntensity={emissiveIntensity}
              roughness={0.4}
              metalness={0.3}
            />
          </mesh>
          <mesh position={[0.028, 0, 0]}>
            <boxGeometry args={[0.03, 0.004, 0.012]} />
            <meshStandardMaterial color="#1d4ed8" emissive="#1e40af" emissiveIntensity={0.6} />
          </mesh>
          <mesh position={[-0.028, 0, 0]}>
            <boxGeometry args={[0.03, 0.004, 0.012]} />
            <meshStandardMaterial color="#1d4ed8" emissive="#1e40af" emissiveIntensity={0.6} />
          </mesh>
        </>
      )}
      {typeKey === "DEBRIS" && (
        <mesh>
          <icosahedronGeometry args={[0.015, 0]} />
          <meshStandardMaterial
            color={style.color}
            emissive={style.emissive}
            emissiveIntensity={emissiveIntensity}
            roughness={0.7}
            flatShading
          />
        </mesh>
      )}
      {typeKey === "ROCKET_BODY" && (
        <mesh>
          <cylinderGeometry args={[0.008, 0.008, 0.032, 8]} />
          <meshStandardMaterial
            color={style.color}
            emissive={style.emissive}
            emissiveIntensity={emissiveIntensity}
            roughness={0.5}
          />
        </mesh>
      )}
      {selected && (
        <mesh rotation={[Math.PI / 2.4, 0, 0]}>
          <torusGeometry args={[0.045, 0.002, 8, 48]} />
          <meshBasicMaterial color="#22d3ee" transparent opacity={0.9} />
        </mesh>
      )}
    </group>
  );
}

function Objects3D({ trackedObjects = [], selectedObjectId = null, highlightedEncounter = null, onSelectObject }) {
  const meshRefs = useRef(new Map());

  const highlightedIds = useMemo(() => {
    if (!highlightedEncounter) return new Set();
    return new Set([highlightedEncounter.objectAId, highlightedEncounter.objectBId]);
  }, [highlightedEncounter]);

  useFrame((_, dt) => {
    const k = 1 - Math.pow(0.001, dt);
    for (let i = 0; i < trackedObjects.length; i++) {
      const obj = trackedObjects[i];
      const g = meshRefs.current.get(obj.id);
      if (!g) continue;
      const [tx, ty, tz] = mapPositionToScene(obj.position, i);
      g.position.x += (tx - g.position.x) * k;
      g.position.y += (ty - g.position.y) * k;
      g.position.z += (tz - g.position.z) * k;
      positionStore.set(obj.id, g.position.clone());
    }
  });

  return (
    <group>
      {trackedObjects.map((obj, i) => (
        <group
          key={obj.id}
          ref={(el) => {
            if (el) meshRefs.current.set(obj.id, el);
            else meshRefs.current.delete(obj.id);
          }}
          position={mapPositionToScene(obj.position, i)}
          onClick={(e) => {
            e.stopPropagation();
            onSelectObject?.(obj);
          }}
          onPointerOver={(e) => {
            e.stopPropagation();
            document.body.style.cursor = "pointer";
          }}
          onPointerOut={() => {
            document.body.style.cursor = "auto";
          }}
        >
          <ObjectMesh
            obj={obj}
            selected={obj.id === selectedObjectId}
            highlighted={highlightedIds.has(obj.id)}
          />
        </group>
      ))}
    </group>
  );
}

function EncounterLines({ selectedEncounter }) {
  const dotRef = useRef(null);
  const lineRef = useRef(null);

  const color = selectedEncounter
    ? Number(selectedEncounter.riskScore) >= 70
      ? "#ef4444"
      : Number(selectedEncounter.riskScore) >= 40
      ? "#f59e0b"
      : "#22d3ee"
    : "#22d3ee";

  const geo = useMemo(
    () => new THREE.BufferGeometry().setFromPoints([new THREE.Vector3(), new THREE.Vector3()]),
    []
  );

  useEffect(() => () => geo.dispose(), [geo]);

  useFrame(({ clock }) => {
    if (!selectedEncounter || !dotRef.current || !lineRef.current) return;
    const a = positionStore.get(selectedEncounter.objectAId);
    const b = positionStore.get(selectedEncounter.objectBId);
    if (!a || !b) return;
    const pos = lineRef.current.geometry.attributes.position;
    pos.setXYZ(0, a.x, a.y, a.z);
    pos.setXYZ(1, b.x, b.y, b.z);
    pos.needsUpdate = true;
    dotRef.current.position.copy(a).lerp(b, 0.5 + Math.sin(clock.elapsedTime * 4) * 0.1);
  });

  if (!selectedEncounter) return null;

  return (
    <>
      <line ref={lineRef} geometry={geo}>
        <lineBasicMaterial color={color} transparent opacity={0.95} />
      </line>
      <mesh ref={dotRef}>
        <sphereGeometry args={[0.015, 16, 16]} />
        <meshBasicMaterial color={color} />
      </mesh>
    </>
  );
}

function ControlsBridge({ resetSignal }) {
  const controls = useThree((s) => s.controls);
  useEffect(() => {
    controls?.saveState?.();
  }, [controls]);
  useEffect(() => {
    if (resetSignal > 0) controls?.reset?.();
  }, [resetSignal, controls]);
  return null;
}

function OrbitVisualization({
  trackedObjects = [],
  selectedObject = null,
  onSelectObject = null,
  selectedEncounter = null,
  isFullScreen = false,
  onToggleFullScreen = null,
}) {
  const [resetSignal, setResetSignal] = useState(0);

  return (
    <div style={{ position: "relative", width: "100%", height: "100%", background: "#02040a" }}>
      <Canvas
        camera={{ position: [0.6, 1.7, 3.3], fov: 42, near: 0.05, far: 300 }}
        dpr={[1, 2]}
        gl={{ antialias: true }}
      >
        <color attach="background" args={["#02040a"]} />
        <ambientLight intensity={0.55} />
        <directionalLight position={[5, 3, 4]} intensity={1.9} />
        <pointLight position={[-4, -2, -3]} intensity={60} distance={30} color="#3b82f6" />
        <Stars radius={90} depth={50} count={5000} factor={4} saturation={0} fade speed={0.6} />

        {/* 3D Realistic Procedural Earth with Atmosphere */}
        <Earth />

        {/* 3D Orbital Path Reference Rings */}
        <OrbitRings />

        {/* Satellites and Debris revolving in 3D */}
        <Objects3D
          trackedObjects={trackedObjects}
          selectedObjectId={selectedObject?.id}
          highlightedEncounter={selectedEncounter}
          onSelectObject={onSelectObject}
        />

        {/* Dynamic Conjunction Lines when an encounter is selected */}
        <EncounterLines selectedEncounter={selectedEncounter} />

        <OrbitControls
          makeDefault
          enableDamping
          dampingFactor={0.08}
          minDistance={1.35}
          maxDistance={14}
          enablePan
          panSpeed={0.6}
        />
        <ControlsBridge resetSignal={resetSignal} />
      </Canvas>

      {/* Top right: View controls */}
      <div style={{ position: "absolute", top: "12px", right: "12px", display: "flex", gap: "8px", zIndex: 20 }}>
        <button
          className="fullscreen-btn"
          style={{ position: "static" }}
          onClick={() => setResetSignal((s) => s + 1)}
          title="Reset 3D camera view"
        >
          <RotateCcw size={14} /> Reset view
        </button>
        {onToggleFullScreen && (
          <button
            className="fullscreen-btn"
            style={{ position: "static" }}
            onClick={onToggleFullScreen}
            title={isFullScreen ? "Exit Fullscreen (Esc)" : "Enter Fullscreen"}
          >
            {isFullScreen ? (
              <>
                <Minimize2 size={14} /> Exit
              </>
            ) : (
              <>
                <Maximize2 size={14} /> Fullscreen
              </>
            )}
          </button>
        )}
      </div>

      {/* Bottom left: Color-coded legend */}
      <div className="orbit-legend orbit-legend-3d">
        <span><i className="legend-blue" style={{ background: "#38bdf8", boxShadow: "0 0 8px rgba(56,189,248,0.8)" }} /> Satellite</span>
        <span><i className="legend-orange" style={{ background: "#fb923c", boxShadow: "0 0 8px rgba(251,146,60,0.8)" }} /> Debris</span>
        <span><i style={{ width: "7px", height: "7px", borderRadius: "2px", background: "#a78bfa", display: "inline-block", boxShadow: "0 0 8px rgba(167,139,250,0.8)" }} /> Rocket body</span>
      </div>

      {/* Bottom right: Interaction hint */}
      <div
        style={{
          position: "absolute",
          bottom: "12px",
          right: "14px",
          fontSize: "10px",
          fontFamily: "monospace",
          color: "#64748b",
          pointerEvents: "none",
          zIndex: 10,
        }}
      >
        Drag to orbit · Scroll to zoom · Right-drag to pan · Click object to inspect
      </div>

      {/* Empty state overlay when simulation hasn't populated objects */}
      {trackedObjects.length === 0 && (
        <div
          style={{
            position: "absolute",
            inset: 0,
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            justifyContent: "center",
            gap: "8px",
            background: "rgba(2, 4, 10, 0.45)",
            pointerEvents: "none",
            zIndex: 15,
          }}
        >
          <Satellite size={32} color="#475569" />
          <strong style={{ color: "#cbd5e1", fontSize: "14px" }}>No objects in scene</strong>
          <small style={{ color: "#64748b", fontSize: "12px" }}>
            Generate a scenario and run the simulation to track objects in 3D.
          </small>
        </div>
      )}
    </div>
  );
}

export default OrbitVisualization;
