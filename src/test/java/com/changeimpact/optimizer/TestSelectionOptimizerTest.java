package com.changeimpact.optimizer;

import com.changeimpact.model.*;
import com.changeimpact.scorer.RiskWeights;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TestSelectionOptimizerTest {

    @Test
    @DisplayName("Should strictly enforce hard execution budget constraint")
    public void testStrictBudgetConstraint() {
        TestSelectionOptimizer optimizer = new TestSelectionOptimizer();

        TestCandidate t1 = new TestCandidate("T1", "T1", null, 100, Set.of("A"), Set.of("A"), 1.0, 0.8, 0.5, 0.5, 0.7);
        TestCandidate t2 = new TestCandidate("T2", "T2", null, 150, Set.of("B"), Set.of("B"), 0.5, 0.6, 0.5, 0.5, 0.5);
        TestCandidate t3 = new TestCandidate("T3", "T3", null, 200, Set.of("C"), Set.of("C"), 0.3, 0.5, 0.5, 0.5, 0.4);

        List<TestCandidate> candidates = List.of(t1, t2, t3);
        long budgetMs = 220; // Enough for T1 (100) + either nothing or another small test, but not T1 + T2 (250)

        TestSelectionResult result = optimizer.selectTests(
                TestSelectionStrategy.RISK_AWARE_BUDGETED,
                candidates,
                candidates,
                Set.of("A", "B", "C"),
                budgetMs,
                RiskWeights.defaultWeights(),
                null
        );

        assertTrue(result.getUsedBudgetMs() <= budgetMs,
                "Used budget (" + result.getUsedBudgetMs() + ") must not exceed budget limit (" + budgetMs + ")");
        assertEquals(1, result.getSelectedTests().size());
        assertEquals("T1", result.getSelectedTests().getFirst().getTestId());
    }

    @Test
    @DisplayName("Should prioritize marginal newly covered components and avoid redundant duplication")
    public void testMarginalCoverageDeduplication() {
        TestSelectionOptimizer optimizer = new TestSelectionOptimizer();

        // T1 covers A and B (cost 30) - high efficiency
        TestCandidate t1 = new TestCandidate("T1", "T1", null, 30, Set.of("A", "B"), Set.of("A", "B"), 1.0, 0.5, 0.66, 0.5, 0.7);
        // T2 covers only A (cost 40) - redundant once T1 is selected
        TestCandidate t2 = new TestCandidate("T2", "T2", null, 40, Set.of("A"), Set.of("A"), 1.0, 0.5, 0.33, 0.5, 0.6);
        // T3 covers C (cost 40) - introduces new coverage!
        TestCandidate t3 = new TestCandidate("T3", "T3", null, 40, Set.of("C"), Set.of("C"), 0.5, 0.5, 0.33, 0.5, 0.55);

        List<TestCandidate> candidates = List.of(t1, t2, t3);
        long budgetMs = 100; // Enough for 2 tests: T1 (30) + T3 (40) = 70ms

        TestSelectionResult result = optimizer.selectTests(
                TestSelectionStrategy.RISK_AWARE_BUDGETED,
                candidates,
                candidates,
                Set.of("A", "B", "C"),
                budgetMs,
                RiskWeights.defaultWeights(),
                null
        );

        assertEquals(2, result.getSelectedTests().size());
        List<String> selectedIds = result.getSelectedTests().stream().map(TestCandidate::getTestId).toList();
        assertTrue(selectedIds.contains("T1"));
        assertTrue(selectedIds.contains("T3"), "T3 should be preferred over redundant T2 due to marginal component coverage");
        assertFalse(selectedIds.contains("T2"));

        // Check that T2 explanation notes redundancy
        SelectionExplanation t2Explanation = result.getExplanations().stream()
                .filter(e -> e.getTestId().equals("T2"))
                .findFirst()
                .orElse(null);

        assertNotNull(t2Explanation);
        assertEquals(SelectionStatus.SKIPPED_REDUNDANT, t2Explanation.getStatus());
    }

    @Test
    @DisplayName("Should handle insufficient budget gracefully without crashing")
    public void testInsufficientBudget() {
        TestSelectionOptimizer optimizer = new TestSelectionOptimizer();
        TestCandidate t1 = new TestCandidate("T1", "T1", null, 100, Set.of("A"), Set.of("A"), 1.0, 0.5, 1.0, 0.5, 0.7);

        TestSelectionResult result = optimizer.selectTests(
                TestSelectionStrategy.RISK_AWARE_BUDGETED,
                List.of(t1),
                List.of(t1),
                Set.of("A"),
                20L, // Budget 20ms < cost 100ms
                RiskWeights.defaultWeights(),
                null
        );

        assertTrue(result.getSelectedTests().isEmpty());
        assertEquals(0, result.getUsedBudgetMs());
        assertEquals(SelectionStatus.SKIPPED_BUDGET_EXCEEDED, result.getExplanations().getFirst().getStatus());
    }
}
