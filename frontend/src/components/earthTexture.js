import * as THREE from "three";

/**
 * Procedural Earth texture (no external assets): ocean gradient, continent
 * blob clusters at approximate geographic positions, polar ice caps and
 * cloud wisps. Rendered once to a canvas and used as a CanvasTexture.
 */

function mulberry32(seed) {
  let a = seed >>> 0;
  return () => {
    a |= 0;
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

const CONTINENTS = [
  // North America
  { lon: -100, lat: 48, r: 22 }, { lon: -85, lat: 38, r: 14 }, { lon: -120, lat: 42, r: 13 },
  { lon: -70, lat: 46, r: 11 }, { lon: -95, lat: 62, r: 15 }, { lon: -140, lat: 64, r: 12 },
  { lon: -55, lat: 52, r: 9 }, { lon: -105, lat: 30, r: 9 }, { lon: -75, lat: 25, r: 6 },
  // Greenland
  { lon: -42, lat: 72, r: 8 },
  // South America
  { lon: -62, lat: -8, r: 13 }, { lon: -66, lat: -22, r: 10 }, { lon: -56, lat: -34, r: 8 },
  { lon: -72, lat: -18, r: 7 }, { lon: -48, lat: -4, r: 8 },
  // Africa
  { lon: 22, lat: 6, r: 15 }, { lon: 26, lat: 18, r: 12 }, { lon: 16, lat: -8, r: 11 },
  { lon: 30, lat: -22, r: 9 }, { lon: 6, lat: 10, r: 9 }, { lon: 44, lat: 4, r: 8 },
  // Eurasia
  { lon: 55, lat: 52, r: 18 }, { lon: 85, lat: 56, r: 16 }, { lon: 115, lat: 48, r: 14 },
  { lon: 35, lat: 48, r: 12 }, { lon: 8, lat: 52, r: 10 }, { lon: 140, lat: 62, r: 12 },
  { lon: 78, lat: 32, r: 11 }, { lon: 105, lat: 28, r: 10 }, { lon: 20, lat: 62, r: 12 },
  { lon: -2, lat: 56, r: 7 }, { lon: 65, lat: 22, r: 8 }, { lon: 100, lat: 14, r: 7 },
  { lon: 122, lat: 34, r: 8 }, { lon: 45, lat: 30, r: 7 },
  // SE Asia / Indonesia
  { lon: 110, lat: -2, r: 8 }, { lon: 125, lat: -4, r: 6 },
  // Australia
  { lon: 134, lat: -25, r: 11 }, { lon: 146, lat: -36, r: 8 }, { lon: 122, lat: -18, r: 7 },
  // New Zealand / Japan / UK
  { lon: 172, lat: -42, r: 5 }, { lon: 138, lat: 37, r: 5 }, { lon: -2, lat: 54, r: 4 },
];

export function makeEarthTexture() {
  const w = 1024;
  const h = 512;
  const canvas = document.createElement("canvas");
  canvas.width = w;
  canvas.height = h;
  const ctx = canvas.getContext("2d");
  if (!ctx) return new THREE.Texture();
  const rand = mulberry32(1337);

  // ocean
  const ocean = ctx.createLinearGradient(0, 0, 0, h);
  ocean.addColorStop(0, "#0a2a5e");
  ocean.addColorStop(0.5, "#0d3a7d");
  ocean.addColorStop(1, "#0a2a5e");
  ctx.fillStyle = ocean;
  ctx.fillRect(0, 0, w, h);

  // subtle ocean noise
  for (let i = 0; i < 900; i++) {
    ctx.fillStyle = `rgba(255,255,255,${rand() * 0.03})`;
    ctx.fillRect(rand() * w, rand() * h, 2, 2);
  }

  const toXY = (lon, lat) => [
    ((lon + 180) / 360) * w,
    ((90 - lat) / 180) * h,
  ];

  // continents: layered radial-gradient blobs (drawn 3x for lon seam wrap)
  for (const c of CONTINENTS) {
    const [cx, cy] = toXY(c.lon, c.lat);
    const R = (c.r / 360) * w;
    for (let i = 0; i < 26; i++) {
      const ang = rand() * Math.PI * 2;
      const dist = rand() * R * 0.75;
      const bx = cx + Math.cos(ang) * dist;
      const by = cy + Math.sin(ang) * dist * 0.55;
      const br = R * (0.16 + rand() * 0.3);
      const tan = rand() > 0.72;
      const green = 90 + Math.floor(rand() * 60);
      const color = tan
        ? "rgba(150,128,74,0.85)"
        : `rgba(${40 + Math.floor(rand() * 30)},${green},${50 + Math.floor(rand() * 30)},0.9)`;
      for (const ox of [-w, 0, w]) {
        const g = ctx.createRadialGradient(bx + ox, by, 0, bx + ox, by, br);
        g.addColorStop(0, color);
        g.addColorStop(1, "rgba(0,0,0,0)");
        ctx.fillStyle = g;
        ctx.beginPath();
        ctx.arc(bx + ox, by, br, 0, Math.PI * 2);
        ctx.fill();
      }
    }
  }

  // polar ice caps
  const ice = ctx.createLinearGradient(0, 0, 0, h);
  ice.addColorStop(0, "rgba(226,240,255,0.95)");
  ice.addColorStop(0.06, "rgba(226,240,255,0.25)");
  ice.addColorStop(0.94, "rgba(226,240,255,0.25)");
  ice.addColorStop(1, "rgba(226,240,255,0.95)");
  ctx.fillStyle = ice;
  ctx.fillRect(0, 0, w, h);

  // cloud wisps
  for (let i = 0; i < 70; i++) {
    const cx = rand() * w;
    const cy = rand() * h;
    const r = 12 + rand() * 46;
    const g = ctx.createRadialGradient(cx, cy, 0, cx, cy, r);
    g.addColorStop(0, `rgba(255,255,255,${0.05 + rand() * 0.1})`);
    g.addColorStop(1, "rgba(255,255,255,0)");
    ctx.fillStyle = g;
    ctx.beginPath();
    ctx.arc(cx, cy, r, 0, Math.PI * 2);
    ctx.fill();
  }

  const tex = new THREE.CanvasTexture(canvas);
  tex.colorSpace = THREE.SRGBColorSpace;
  tex.anisotropy = 4;
  return tex;
}

