package com.changeimpact.analyzer;

import com.changeimpact.coverage.CoverageRepository;
import com.changeimpact.graph.DependencyGraph;
import com.changeimpact.model.ComponentImpact;
import com.changeimpact.model.ImpactReport;

import java.util.*;

/**
 * Analyzes changes to determine affected components via reverse reachability,
 * identifies candidate tests, and produces an explainable impact report.
 */
public class ChangeImpactAnalyzer {

    public ChangeImpactAnalyzer() {
    }

    /**
     * Analyze the impact of a set of changed components.
     *
     * @param changedComponentIds set of directly changed component IDs
     * @param graph               dependency graph
     * @param coverage            coverage repository
     * @return explainable ImpactReport
     */
    public ImpactReport analyze(
            Set<String> changedComponentIds,
            DependencyGraph graph,
            CoverageRepository coverage) {
        long startTime = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();

        if (graph == null) {
            throw new IllegalArgumentException("DependencyGraph cannot be null");
        }

        if (changedComponentIds == null || changedComponentIds.isEmpty()) {
            warnings.add("No changed components provided.");
            return new ImpactReport(
                    Collections.emptySet(),
                    Collections.emptyMap(),
                    Collections.emptySet(),
                    false,
                    Collections.emptyList(),
                    warnings,
                    System.currentTimeMillis() - startTime
            );
        }

        // Verify which changed components exist in the graph
        Set<String> validChangedIds = new LinkedHashSet<>();
        for (String id : changedComponentIds) {
            if (graph.hasNode(id)) {
                validChangedIds.add(id);
            } else {
                warnings.add("Changed component '" + id + "' does not exist in dependency graph.");
            }
        }

        // Check for graph cycles
        List<List<String>> cycles = graph.detectCycles();
        boolean cycleDetected = !cycles.isEmpty();
        if (cycleDetected) {
            warnings.add("Graph contains " + cycles.size() + " dependency cycle(s). Cycle-safe traversal applied.");
        }

        // Traverse reverse dependencies to find affected components
        Map<String, ComponentImpact> impacted = graph.getReverseReachable(validChangedIds);

        // Identify candidate tests that cover any of the impacted components
        Set<String> candidateTests = new LinkedHashSet<>();
        if (coverage != null) {
            for (String compId : impacted.keySet()) {
                Set<String> tests = coverage.getTestsExercising(compId);
                candidateTests.addAll(tests);
            }
        } else {
            warnings.add("No CoverageRepository provided; candidate tests cannot be resolved.");
        }

        long durationMs = System.currentTimeMillis() - startTime;
        return new ImpactReport(
                validChangedIds,
                impacted,
                candidateTests,
                cycleDetected,
                cycles,
                warnings,
                durationMs
        );
    }
}
