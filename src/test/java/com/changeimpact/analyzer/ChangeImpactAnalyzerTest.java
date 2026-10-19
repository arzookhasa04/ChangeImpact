package com.changeimpact.analyzer;

import com.changeimpact.coverage.CoverageRepository;
import com.changeimpact.graph.DependencyGraph;
import com.changeimpact.model.CoverageSource;
import com.changeimpact.model.ImpactReport;
import com.changeimpact.model.Node;
import com.changeimpact.model.NodeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ChangeImpactAnalyzerTest {

    @Test
    @DisplayName("Should identify all affected components and candidate tests")
    public void testImpactAnalysisPipeline() {
        DependencyGraph graph = new DependencyGraph();
        CoverageRepository coverage = new CoverageRepository();

        graph.addNode(new Node("DB", NodeType.COMPONENT));
        graph.addNode(new Node("Service", NodeType.COMPONENT));
        graph.addNode(new Node("Controller", NodeType.COMPONENT));

        // Controller -> Service -> DB
        graph.addEdge("Controller", "Service");
        graph.addEdge("Service", "DB");

        // Tests
        coverage.addCoverage("TestDB", "DB", CoverageSource.MEASURED);
        coverage.addCoverage("TestService", "Service", CoverageSource.MEASURED);
        coverage.addCoverage("TestController", "Controller", CoverageSource.MEASURED);

        ChangeImpactAnalyzer analyzer = new ChangeImpactAnalyzer();
        ImpactReport report = analyzer.analyze(Set.of("DB"), graph, coverage);

        assertEquals(3, report.getImpactedComponents().size());
        assertTrue(report.getImpactedComponents().containsKey("DB"));
        assertTrue(report.getImpactedComponents().containsKey("Service"));
        assertTrue(report.getImpactedComponents().containsKey("Controller"));

        assertEquals(3, report.getCandidateTestIds().size());
        assertTrue(report.getCandidateTestIds().contains("TestDB"));
        assertTrue(report.getCandidateTestIds().contains("TestService"));
        assertTrue(report.getCandidateTestIds().contains("TestController"));
    }

    @Test
    @DisplayName("Should handle missing components with explicit warnings")
    public void testMissingComponentWarning() {
        DependencyGraph graph = new DependencyGraph();
        CoverageRepository coverage = new CoverageRepository();

        ChangeImpactAnalyzer analyzer = new ChangeImpactAnalyzer();
        ImpactReport report = analyzer.analyze(Set.of("GhostService"), graph, coverage);

        assertTrue(report.getImpactedComponents().isEmpty());
        assertFalse(report.getWarnings().isEmpty());
        assertTrue(report.getWarnings().getFirst().contains("GhostService"));
    }
}
