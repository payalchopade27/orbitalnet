package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrbitalGraphTest {

    private Encounter createAssessedEncounter(
            String id,
            String objectA,
            String objectB,
            int totalScore) {

        Encounter enc = new Encounter(
                id, "RUN-01", objectA, objectB,
                15.0, 500.0, 5.0, 1
        );
        RiskScore score = new RiskScore(
                totalScore, 0.85, 0.33, 0.86,
                RiskScore.computeLabel(totalScore),
                "Explanation for " + id
        );
        enc.assess(score);
        return enc;
    }

    @Test
    void emptyGraphState() {
        OrbitalGraph graph = new OrbitalGraph();
        assertEquals(0, graph.vertexCount());
        assertEquals(0, graph.edgeCount());
        assertTrue(graph.getVertices().isEmpty());
        assertFalse(graph.hasVertex("SAT-01"));
        assertFalse(graph.hasEdge("SAT-01", "DEB-01"));
        assertEquals(0, graph.getDegree("SAT-01"));
        assertTrue(graph.getNeighbors("SAT-01").isEmpty());
    }

    @Test
    void addVerticesAndUndirectedEdges() {
        OrbitalGraph graph = new OrbitalGraph();
        graph.addVertex("SAT-01");
        graph.addVertex("DEB-01");

        assertEquals(2, graph.vertexCount());
        assertEquals(0, graph.edgeCount());
        assertTrue(graph.hasVertex("SAT-01"));
        assertTrue(graph.hasVertex("DEB-01"));

        Encounter encounter = createAssessedEncounter("ENC-01", "SAT-01", "DEB-01", 85);
        graph.addEdge(encounter);

        assertEquals(2, graph.vertexCount());
        assertEquals(1, graph.edgeCount());
        assertTrue(graph.hasEdge("SAT-01", "DEB-01"));
        assertTrue(graph.hasEdge("DEB-01", "SAT-01"));

        assertEquals(1, graph.getDegree("SAT-01"));
        assertEquals(1, graph.getDegree("DEB-01"));
        assertEquals(List.of("DEB-01"), graph.getNeighbors("SAT-01"));
        assertEquals(List.of("SAT-01"), graph.getNeighbors("DEB-01"));

        // Adding duplicate edge should not duplicate edgeCount
        graph.addEdge(encounter);
        assertEquals(1, graph.edgeCount());
    }

    @Test
    void validationRejectsInvalidInputs() {
        OrbitalGraph graph = new OrbitalGraph();

        assertThrows(IllegalArgumentException.class, () -> graph.addVertex(null));
        assertThrows(IllegalArgumentException.class, () -> graph.addVertex("  "));
        assertThrows(NullPointerException.class, () -> graph.addEdge(null));
        assertThrows(IllegalArgumentException.class, () ->
                graph.addEdge("SAT-01", "SAT-01", null, 50.0)); // self-edge rejected
    }

    @Test
    void bfsTraversesLevelByLevel() {
        // Construct a graph:
        //      A --- B --- D
        //      |
        //      C --- E
        OrbitalGraph graph = new OrbitalGraph();
        graph.addEdge("A", "B", null, 10);
        graph.addEdge("A", "C", null, 10);
        graph.addEdge("B", "D", null, 10);
        graph.addEdge("C", "E", null, 10);

        List<String> visited = graph.bfs("A");
        assertEquals(List.of("A", "B", "C", "D", "E"), visited);

        // BFS on non-existent node returns empty list
        assertTrue(graph.bfs("UNKNOWN").isEmpty());
    }

    @Test
    void bfsCalculatesShortestHopDistance() {
        // Linear path: A - B - C - D, and disconnected node X
        OrbitalGraph graph = new OrbitalGraph();
        graph.addEdge("A", "B", null, 1);
        graph.addEdge("B", "C", null, 1);
        graph.addEdge("C", "D", null, 1);
        graph.addVertex("X");

        assertEquals(0, graph.getShortestHopDistance("A", "A"));
        assertEquals(1, graph.getShortestHopDistance("A", "B"));
        assertEquals(2, graph.getShortestHopDistance("A", "C"));
        assertEquals(3, graph.getShortestHopDistance("A", "D"));

        // Unreachable vertex
        assertEquals(-1, graph.getShortestHopDistance("A", "X"));
        // Unknown vertex
        assertEquals(-1, graph.getShortestHopDistance("A", "Z"));
    }

    @Test
    void dfsTraversesBranchesAndHandlesCycles() {
        // Cycle: A - B - C - A, plus branch C - D
        OrbitalGraph graph = new OrbitalGraph();
        graph.addEdge("A", "B", null, 1);
        graph.addEdge("B", "C", null, 1);
        graph.addEdge("C", "A", null, 1);
        graph.addEdge("C", "D", null, 1);

        List<String> dfsOrder = graph.dfs("A");
        assertEquals(4, dfsOrder.size());
        assertEquals("A", dfsOrder.get(0));
        assertTrue(dfsOrder.containsAll(List.of("A", "B", "C", "D")));

        assertTrue(graph.dfs("UNKNOWN").isEmpty());
    }

    @Test
    void findsConnectedRiskClustersFromSimulationEncounters() {
        // Cluster 1: SAT-01 <-> DEB-01 <-> DEB-02 (cascading risk cluster)
        // Cluster 2: SAT-02 <-> DEB-03 (isolated pair)
        // Disconnected node: SAT-03 (clear orbit, no threats)
        Encounter e1 = createAssessedEncounter("E1", "SAT-01", "DEB-01", 90);
        Encounter e2 = createAssessedEncounter("E2", "DEB-01", "DEB-02", 70);
        Encounter e3 = createAssessedEncounter("E3", "SAT-02", "DEB-03", 40);

        OrbitalGraph graph = OrbitalGraph.fromEncounters(Arrays.asList(e1, e2, e3));
        graph.addVertex("SAT-03"); // Clean satellite with no threats

        assertEquals(6, graph.vertexCount());
        assertEquals(3, graph.edgeCount());

        List<List<String>> clusters = graph.findConnectedRiskClusters();
        assertEquals(3, clusters.size());

        // Cluster 1 has 3 objects
        assertTrue(clusters.stream().anyMatch(c -> c.size() == 3
                && c.containsAll(List.of("SAT-01", "DEB-01", "DEB-02"))));

        // Cluster 2 has 2 objects
        assertTrue(clusters.stream().anyMatch(c -> c.size() == 2
                && c.containsAll(List.of("SAT-02", "DEB-03"))));

        // Cluster 3 has 1 object (clean orbit)
        assertTrue(clusters.stream().anyMatch(c -> c.size() == 1
                && c.contains("SAT-03")));
    }
}

