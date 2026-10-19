package com.changeimpact.graph;

import com.changeimpact.model.ComponentImpact;
import com.changeimpact.model.DependencyEdge;
import com.changeimpact.model.EdgeType;
import com.changeimpact.model.Node;
import com.changeimpact.model.NodeType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

/**
 * Directed dependency graph using standard Java collections.
 * <p>
 * An edge {@code A -> B} signifies that entity {@code A} depends on, calls, or exercises {@code B}.
 * For change impact analysis, if {@code B} changes, entity {@code A} is potentially affected.
 * Therefore, impact propagation follows reverse edges.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DependencyGraph {

    private final Map<String, Node> nodes;
    private final Set<DependencyEdge> edges;

    // Adjacency lists for fast lookup
    @JsonIgnore
    private final Map<String, Set<String>> forwardAdjacency;
    @JsonIgnore
    private final Map<String, Set<String>> reverseAdjacency;
    @JsonIgnore
    private final Map<String, Map<String, DependencyEdge>> edgeLookup;

    public DependencyGraph() {
        this.nodes = new LinkedHashMap<>();
        this.edges = new LinkedHashSet<>();
        this.forwardAdjacency = new HashMap<>();
        this.reverseAdjacency = new HashMap<>();
        this.edgeLookup = new HashMap<>();
    }

    @JsonCreator
    public DependencyGraph(
            @JsonProperty("nodes") List<Node> nodeList,
            @JsonProperty("edges") List<DependencyEdge> edgeList) {
        this();
        if (nodeList != null) {
            for (Node n : nodeList) {
                addNode(n);
            }
        }
        if (edgeList != null) {
            for (DependencyEdge e : edgeList) {
                addEdge(e);
            }
        }
    }

    /**
     * Add a node to the graph. If node already exists, it is replaced.
     */
    public synchronized void addNode(Node node) {
        Objects.requireNonNull(node, "Node cannot be null");
        nodes.put(node.getId(), node);
        forwardAdjacency.computeIfAbsent(node.getId(), k -> new LinkedHashSet<>());
        reverseAdjacency.computeIfAbsent(node.getId(), k -> new LinkedHashSet<>());
    }

    /**
     * Add an edge to the graph. Creates placeholder nodes if they do not exist yet.
     */
    public synchronized void addEdge(DependencyEdge edge) {
        Objects.requireNonNull(edge, "Edge cannot be null");
        nodes.computeIfAbsent(edge.getFromNodeId(), id -> new Node(id, NodeType.COMPONENT));
        nodes.computeIfAbsent(edge.getToNodeId(), id -> new Node(id, NodeType.COMPONENT));

        edges.add(edge);
        forwardAdjacency.computeIfAbsent(edge.getFromNodeId(), k -> new LinkedHashSet<>()).add(edge.getToNodeId());
        reverseAdjacency.computeIfAbsent(edge.getToNodeId(), k -> new LinkedHashSet<>()).add(edge.getFromNodeId());
        edgeLookup.computeIfAbsent(edge.getFromNodeId(), k -> new HashMap<>()).put(edge.getToNodeId(), edge);
    }

    public synchronized void addEdge(String fromId, String toId, EdgeType type) {
        addEdge(new DependencyEdge(fromId, toId, type));
    }

    public synchronized void addEdge(String fromId, String toId) {
        addEdge(new DependencyEdge(fromId, toId, EdgeType.DEPENDS_ON));
    }

    public Node getNode(String id) {
        return nodes.get(id);
    }

    public boolean hasNode(String id) {
        return nodes.containsKey(id);
    }

    @JsonProperty("nodes")
    public List<Node> getNodes() {
        return new ArrayList<>(nodes.values());
    }

    @JsonProperty("edges")
    public List<DependencyEdge> getEdges() {
        return new ArrayList<>(edges);
    }

    public Set<String> getForwardNeighbors(String id) {
        return Collections.unmodifiableSet(forwardAdjacency.getOrDefault(id, Collections.emptySet()));
    }

    public Set<String> getReverseNeighbors(String id) {
        return Collections.unmodifiableSet(reverseAdjacency.getOrDefault(id, Collections.emptySet()));
    }

    public DependencyEdge getEdge(String fromId, String toId) {
        Map<String, DependencyEdge> targets = edgeLookup.get(fromId);
        return targets != null ? targets.get(toId) : null;
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public int getEdgeCount() {
        return edges.size();
    }

    /**
     * Compute reverse reachability from a set of changed component IDs.
     * <p>
     * If component {@code C} changes, any component {@code X} that depends directly or
     * transitively on {@code C} is affected.
     * Traversal is cycle-safe using BFS with a visited set.
     *
     * @param changedIds collection of directly changed component IDs
     * @return map of affected component ID to ComponentImpact containing distance and path
     */
    public Map<String, ComponentImpact> getReverseReachable(Collection<String> changedIds) {
        return getReverseReachable(changedIds, true);
    }

    public Map<String, ComponentImpact> getReverseReachable(Collection<String> changedIds, boolean excludeTestNodes) {
        if (changedIds == null || changedIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, ComponentImpact> impacts = new LinkedHashMap<>();
        Queue<String> queue = new ArrayDeque<>();
        Map<String, List<String>> paths = new HashMap<>();
        Map<String, Integer> distances = new HashMap<>();

        // Initialize with changed components
        for (String id : changedIds) {
            String trimmed = id.trim();
            if (nodes.containsKey(trimmed)) {
                Node n = nodes.get(trimmed);
                if (excludeTestNodes && n != null && n.getType() == NodeType.TEST) {
                    continue;
                }
                impacts.put(trimmed, new ComponentImpact(trimmed, true, 0, List.of(trimmed)));
                queue.add(trimmed);
                paths.put(trimmed, List.of(trimmed));
                distances.put(trimmed, 0);
            }
        }

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentDist = distances.get(current);
            List<String> currentPath = paths.get(current);

            // Who depends on `current`?
            Set<String> dependents = reverseAdjacency.getOrDefault(current, Collections.emptySet());
            for (String dependent : dependents) {
                Node depNode = nodes.get(dependent);
                if (excludeTestNodes && depNode != null && depNode.getType() == NodeType.TEST) {
                    // Test nodes are not production components
                    continue;
                }

                if (!impacts.containsKey(dependent)) {
                    int newDist = currentDist + 1;
                    List<String> newPath = new ArrayList<>(currentPath);
                    newPath.add(dependent);

                    impacts.put(dependent, new ComponentImpact(dependent, false, newDist, newPath));
                    distances.put(dependent, newDist);
                    paths.put(dependent, newPath);
                    queue.add(dependent);
                }
            }
        }

        return impacts;
    }

    /**
     * Cycle detection using Tarjan's Strongly Connected Components (SCC) algorithm.
     * Any SCC with more than 1 node (or 1 node with a self-loop) is a cycle.
     *
     * @return list of detected cycles (each cycle is a list of node IDs forming the cycle)
     */
    public List<List<String>> detectCycles() {
        List<List<String>> cycles = new ArrayList<>();
        Map<String, Integer> indices = new HashMap<>();
        Map<String, Integer> lowlinks = new HashMap<>();
        Deque<String> stack = new ArrayDeque<>();
        Set<String> onStack = new HashSet<>();
        int[] index = {0};

        for (String nodeId : nodes.keySet()) {
            if (!indices.containsKey(nodeId)) {
                strongConnect(nodeId, indices, lowlinks, stack, onStack, index, cycles);
            }
        }

        return cycles;
    }

    private void strongConnect(
            String u,
            Map<String, Integer> indices,
            Map<String, Integer> lowlinks,
            Deque<String> stack,
            Set<String> onStack,
            int[] index,
            List<List<String>> cycles) {
        indices.put(u, index[0]);
        lowlinks.put(u, index[0]);
        index[0]++;
        stack.push(u);
        onStack.add(u);

        for (String v : forwardAdjacency.getOrDefault(u, Collections.emptySet())) {
            if (!indices.containsKey(v)) {
                strongConnect(v, indices, lowlinks, stack, onStack, index, cycles);
                lowlinks.put(u, Math.min(lowlinks.get(u), lowlinks.get(v)));
            } else if (onStack.contains(v)) {
                lowlinks.put(u, Math.min(lowlinks.get(u), indices.get(v)));
            }
        }

        if (Objects.equals(lowlinks.get(u), indices.get(u))) {
            List<String> scc = new ArrayList<>();
            String w;
            do {
                w = stack.pop();
                onStack.remove(w);
                scc.add(w);
            } while (!Objects.equals(w, u));

            // A cycle must have >= 2 nodes or a self-loop
            if (scc.size() > 1) {
                cycles.add(scc);
            } else if (scc.size() == 1) {
                String single = scc.getFirst();
                if (forwardAdjacency.getOrDefault(single, Collections.emptySet()).contains(single)) {
                    cycles.add(scc);
                }
            }
        }
    }
}
