/**
 * Shared mutable store of current scene-space positions, written every frame
 * by Objects3D and read by EncounterLines so conjunction lines track the
 * rendered meshes without triggering React re-renders.
 */
export const positionStore = new Map();
