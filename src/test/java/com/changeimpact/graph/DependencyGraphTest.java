package com.changeimpact.graph;

import com.changeimpact.model.ComponentImpact;
import com.changeimpact.model.DependencyEdge;
import com.changeimpact.model.EdgeType;
import com.changeimpact.model.Node;
import com.changeimpact.model.NodeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class DependencyGraphTest {

    @Test
    @DisplayName("Should add nodes and query properties correctly")
    public void testAddAndQueryNodes() {
        DependencyGraph graph = new DependencyGraph();
        Node n1 = new Node("A", "Service A", NodeType.COMPONENT, "src/A.java");
        graph.addNode(n1);

        assertTrue(graph.hasNode("A"));
        assertEquals("Service A", graph.getNode("A").getName());
        assertEquals(1, graph.getNodeCount());
    }

    @Test
    @DisplayName("Should record directed dependency edges and track forward and reverse neighbors")
    public void testEdgesAndAdjacency() {
        DependencyGraph graph = new DependencyGraph();
        // A depends on B (A calls B)
        graph.addEdge("A", "B", EdgeType.CALLS);

        assertEquals(2, graph.getNodeCount());
        assertEquals(1, graph.getEdgeCount());

        assertTrue(graph.getForwardNeighbors("A").contains("B"));
        assertTrue(graph.getReverseNeighbors("B").contains("A"));
        assertFalse(graph.getForwardNeighbors("B").contains("A"));
    }

    @Test
    @DisplayName("Should perform cycle-safe reverse reachability when target changes")
    public void testReverseReachabilityLinear() {
        DependencyGraph graph = new DependencyGraph();
        // C depends on B, B depends on A: C -> B -> A
        // If A changes, B and C are impacted.
        graph.addEdge("C", "B");
        graph.addEdge("B", "A");

        Map<String, ComponentImpact> impact = graph.getReverseReachable(Set.of("A"));

        assertEquals(3, impact.size());
        assertTrue(impact.containsKey("A"));
        assertTrue(impact.containsKey("B"));
        assertTrue(impact.containsKey("C"));

        assertEquals(0, impact.get("A").getDistance());
        assertTrue(impact.get("A").isDirectChange());

        assertEquals(1, impact.get("B").getDistance());
        assertFalse(impact.get("B").isDirectChange());

        assertEquals(2, impact.get("C").getDistance());
        assertFalse(impact.get("C").isDirectChange());
        assertEquals(List.of("A", "B", "C"), impact.get("C").getImpactPath());
    }

    @Test
    @DisplayName("Should detect dependency cycles and handle cyclic reachability safely without infinite loops")
    public void testCycleDetectionAndSafeTraversal() {
        DependencyGraph graph = new DependencyGraph();
        // Cycle: A -> B -> C -> A
        graph.addEdge("A", "B");
        graph.addEdge("B", "C");
        graph.addEdge("C", "A");

        List<List<String>> cycles = graph.detectCycles();
        assertFalse(cycles.isEmpty(), "Cycle must be detected");
        assertEquals(1, cycles.size());
        assertEquals(3, cycles.getFirst().size());

        // Reverse reachability from A must terminate cleanly
        Map<String, ComponentImpact> impact = graph.getReverseReachable(Set.of("A"));
        assertEquals(3, impact.size());
        assertTrue(impact.containsKey("A"));
        assertTrue(impact.containsKey("B"));
        assertTrue(impact.containsKey("C"));
    }

    @Test
    @DisplayName("Should detect self-loop cycle")
    public void testSelfLoop() {
        DependencyGraph graph = new DependencyGraph();
        graph.addEdge("A", "A");

        List<List<String>> cycles = graph.detectCycles();
        assertEquals(1, cycles.size());
    }

    @Test
    @DisplayName("Should handle empty or null queries safely")
    public void testEmptySafety() {
        DependencyGraph graph = new DependencyGraph();
        assertTrue(graph.getReverseReachable(null).isEmpty());
        assertTrue(graph.getReverseReachable(Set.of()).isEmpty());
        assertTrue(graph.getReverseReachable(Set.of("NON_EXISTENT")).isEmpty());
    }
}
