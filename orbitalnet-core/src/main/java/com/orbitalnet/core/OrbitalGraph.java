package com.orbitalnet.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Adjacency-list graph representing orbital proximity and collision risk relationships.
 *
 * <p>Vertices represent {@link SpaceObject} IDs.
 * Edges represent close-approach encounters between pairs of objects, weighted by risk score.
 *
 * <p>Includes custom Breadth-First Search (BFS) and Depth-First Search (DFS) algorithms
 * for multi-threat network analysis, shortest-path degree calculation, and connected risk cluster detection.
 */
public class OrbitalGraph {

    /**
     * Immutable edge in the orbital graph.
     */
    public static final class Edge {
        private final String targetId;
        private final Encounter encounter;
        private final double weight;

        public Edge(String targetId, Encounter encounter, double weight) {
            this.targetId = Objects.requireNonNull(targetId, "Target ID cannot be null.");
            this.encounter = encounter;
            this.weight = weight;
        }

        public String getTargetId() {
            return targetId;
        }

        public Encounter getEncounter() {
            return encounter;
        }

        public double getWeight() {
            return weight;
        }

        @Override
        public String toString() {
            return "Edge(target=" + targetId + ", weight=" + weight + ")";
        }
    }

    private final Map<String, List<Edge>> adjacencyList = new LinkedHashMap<>();
    private int edgeCount = 0;

    public OrbitalGraph() {
    }

    /**
     * Constructs an orbital graph initialized with the given encounters.
     */
    public static OrbitalGraph fromEncounters(Collection<Encounter> encounters) {
        OrbitalGraph graph = new OrbitalGraph();
        if (encounters != null) {
            for (Encounter encounter : encounters) {
                if (encounter != null) {
                    graph.addEdge(encounter);
                }
            }
        }
        return graph;
    }

    /**
     * Adds a vertex to the graph if it is not already present.
     *
     * @param objectId unique space object ID
     */
    public void addVertex(String objectId) {
        if (objectId == null || objectId.isBlank()) {
            throw new IllegalArgumentException("Vertex ID cannot be null or blank.");
        }
        adjacencyList.putIfAbsent(objectId, new ArrayList<>());
    }

    /**
     * Adds an undirected encounter edge between two space objects.
     */
    public void addEdge(Encounter encounter) {
        Objects.requireNonNull(encounter, "Encounter cannot be null.");
        String a = encounter.getObjectAId();
        String b = encounter.getObjectBId();
        double weight = encounter.getRiskScore() != null
                ? encounter.getRiskScore().getTotalScore()
                : 0.0;

        addEdge(a, b, encounter, weight);
    }

    /**
     * Adds an undirected edge between vertices {@code u} and {@code v}.
     */
    public void addEdge(String u, String v, Encounter encounter, double weight) {
        if (u == null || u.isBlank() || v == null || v.isBlank()) {
            throw new IllegalArgumentException("Vertex IDs cannot be null or blank.");
        }
        if (u.equals(v)) {
            throw new IllegalArgumentException("Self-edges are not allowed in the encounter graph.");
        }

        addVertex(u);
        addVertex(v);

        // Check for existing edge to prevent duplicate undirected edges
        if (!hasEdge(u, v)) {
            adjacencyList.get(u).add(new Edge(v, encounter, weight));
            adjacencyList.get(v).add(new Edge(u, encounter, weight));
            edgeCount++;
        }
    }

    public boolean hasVertex(String objectId) {
        return adjacencyList.containsKey(objectId);
    }

    public boolean hasEdge(String u, String v) {
        List<Edge> edges = adjacencyList.get(u);
        if (edges == null) {
            return false;
        }
        for (Edge edge : edges) {
            if (edge.getTargetId().equals(v)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getNeighbors(String objectId) {
        List<Edge> edges = adjacencyList.get(objectId);
        if (edges == null) {
            return Collections.emptyList();
        }
        List<String> neighbors = new ArrayList<>(edges.size());
        for (Edge edge : edges) {
            neighbors.add(edge.getTargetId());
        }
        return neighbors;
    }

    public List<Edge> getEdges(String objectId) {
        List<Edge> edges = adjacencyList.get(objectId);
        if (edges == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(edges);
    }

    public int getDegree(String objectId) {
        List<Edge> edges = adjacencyList.get(objectId);
        return edges == null ? 0 : edges.size();
    }

    public Set<String> getVertices() {
        return Collections.unmodifiableSet(adjacencyList.keySet());
    }

    public int vertexCount() {
        return adjacencyList.size();
    }

    public int edgeCount() {
        return edgeCount;
    }

    // =========================================================================
    // Breadth-First Search (BFS) using CustomQueue
    // =========================================================================

    /**
     * Performs a Breadth-First Search starting from {@code startId} using {@link CustomQueue}.
     *
     * <p>Time complexity: O(V + E).
     *
     * @param startId starting space object ID
     * @return list of object IDs in the order they were visited
     */
    public List<String> bfs(String startId) {
        if (!hasVertex(startId)) {
            return Collections.emptyList();
        }

        List<String> visitedOrder = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        CustomQueue<String> queue = new CustomQueue<>();

        visited.add(startId);
        queue.enqueue(startId);

        while (!queue.isEmpty()) {
            String current = queue.dequeue();
            visitedOrder.add(current);

            for (String neighbor : getNeighbors(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.enqueue(neighbor);
                }
            }
        }

        return visitedOrder;
    }

    /**
     * Computes the unweighted shortest-path distance (number of encounter hops) between two objects via BFS.
     *
     * @return minimum hops between {@code sourceId} and {@code targetId}, or -1 if unreachable
     */
    public int getShortestHopDistance(String sourceId, String targetId) {
        if (!hasVertex(sourceId) || !hasVertex(targetId)) {
            return -1;
        }
        if (sourceId.equals(targetId)) {
            return 0;
        }

        CustomQueue<String> queue = new CustomQueue<>();
        Map<String, Integer> distance = new LinkedHashMap<>();

        distance.put(sourceId, 0);
        queue.enqueue(sourceId);

        while (!queue.isEmpty()) {
            String current = queue.dequeue();
            int currentDist = distance.get(current);

            for (String neighbor : getNeighbors(current)) {
                if (!distance.containsKey(neighbor)) {
                    distance.put(neighbor, currentDist + 1);
                    if (neighbor.equals(targetId)) {
                        return currentDist + 1;
                    }
                    queue.enqueue(neighbor);
                }
            }
        }

        return -1;
    }

    // =========================================================================
    // Depth-First Search (DFS) using CustomStack
    // =========================================================================

    /**
     * Performs an iterative Depth-First Search starting from {@code startId} using {@link CustomStack}.
     *
     * <p>Time complexity: O(V + E).
     *
     * @param startId starting space object ID
     * @return list of object IDs in the order they were visited
     */
    public List<String> dfs(String startId) {
        if (!hasVertex(startId)) {
            return Collections.emptyList();
        }

        List<String> visitedOrder = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        CustomStack<String> stack = new CustomStack<>();

        stack.push(startId);

        while (!stack.isEmpty()) {
            String current = stack.pop();

            if (!visited.contains(current)) {
                visited.add(current);
                visitedOrder.add(current);

                // Push neighbors in reverse order to explore in forward natural order
                List<String> neighbors = getNeighbors(current);
                for (int i = neighbors.size() - 1; i >= 0; i--) {
                    String neighbor = neighbors.get(i);
                    if (!visited.contains(neighbor)) {
                        stack.push(neighbor);
                    }
                }
            }
        }

        return visitedOrder;
    }

    /**
     * Identifies all connected risk clusters (connected components) in the encounter graph via DFS.
     *
     * @return list of connected clusters, each containing the space object IDs in that cluster
     */
    public List<List<String>> findConnectedRiskClusters() {
        List<List<String>> clusters = new ArrayList<>();
        Set<String> globalVisited = new LinkedHashSet<>();

        for (String vertex : adjacencyList.keySet()) {
            if (!globalVisited.contains(vertex)) {
                List<String> cluster = dfs(vertex);
                globalVisited.addAll(cluster);
                clusters.add(cluster);
            }
        }

        return clusters;
    }
}

