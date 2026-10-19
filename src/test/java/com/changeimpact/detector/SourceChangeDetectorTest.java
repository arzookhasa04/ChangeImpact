package com.changeimpact.detector;

import com.changeimpact.graph.DependencyGraph;
import com.changeimpact.model.Node;
import com.changeimpact.model.NodeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SourceChangeDetectorTest {

    @Test
    @DisplayName("Should parse git status porcelain lines accurately")
    public void testParseGitLine() {
        SourceChangeDetector detector = new SourceChangeDetector();

        FileChange fc1 = detector.parseGitLine("M src/main/java/PaymentService.java");
        assertNotNull(fc1);
        assertEquals("src/main/java/PaymentService.java", fc1.getFilePath());
        assertEquals(FileChangeType.MODIFIED, fc1.getChangeType());

        FileChange fc2 = detector.parseGitLine("A  src/main/java/NewService.java");
        assertNotNull(fc2);
        assertEquals(FileChangeType.ADDED, fc2.getChangeType());

        FileChange fc3 = detector.parseGitLine("D src/main/java/OldService.java");
        assertNotNull(fc3);
        assertEquals(FileChangeType.DELETED, fc3.getChangeType());

        FileChange fc4 = detector.parseGitLine("R  old/Path.java -> new/Path.java");
        assertNotNull(fc4);
        assertEquals(FileChangeType.RENAMED, fc4.getChangeType());
        assertEquals("new/Path.java", fc4.getFilePath());
        assertEquals("old/Path.java", fc4.getOldFilePath());
    }

    @Test
    @DisplayName("Should map detected file changes to graph component IDs")
    public void testMapChangesToComponents() {
        SourceChangeDetector detector = new SourceChangeDetector();
        DependencyGraph graph = new DependencyGraph();

        graph.addNode(new Node("PaymentService", "PaymentService", NodeType.COMPONENT, "src/main/java/com/example/PaymentService.java"));
        graph.addNode(new Node("OrderService", "OrderService", NodeType.COMPONENT, "src/main/java/com/example/OrderService.java"));

        List<FileChange> changes = List.of(
                new FileChange("src/main/java/com/example/PaymentService.java"),
                new FileChange("OrderService.java"),
                new FileChange("unrelated/Random.java")
        );

        Set<String> mapped = detector.mapChangesToComponents(changes, graph);
        assertTrue(mapped.contains("PaymentService"));
        assertTrue(mapped.contains("OrderService"));
        assertFalse(mapped.contains("Random"));
    }
}
