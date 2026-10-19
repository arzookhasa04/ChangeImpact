package com.changeimpact.scorer;

import com.changeimpact.coverage.CoverageRepository;
import com.changeimpact.history.HistoryStore;
import com.changeimpact.model.ComponentImpact;
import com.changeimpact.model.CoverageSource;
import com.changeimpact.model.TestCandidate;
import com.changeimpact.model.TestHistoryRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class RiskScorerTest {

    @Test
    @DisplayName("Should exactly match hand-calculated risk scoring equation")
    public void testHandCalculatedRiskEquation() {
        RiskWeights weights = new RiskWeights(0.35, 0.25, 0.25, 0.15);
        // Use non-recency smoothed failure rate for exact analytical check: (failures + 1) / (runs + 2)
        RiskScorer scorer = new RiskScorer(weights, 0.85, false);

        CoverageRepository coverage = new CoverageRepository();
        coverage.addCoverage("T1", "CompA", CoverageSource.MEASURED);

        HistoryStore history = new HistoryStore();
        // 3 historical runs: 1 failure, 2 passes -> smoothed = (1+1)/(3+2) = 2/5 = 0.40
        history.addRecord(new TestHistoryRecord("T1", true, 50));
        history.addRecord(new TestHistoryRecord("T1", false, 50));
        history.addRecord(new TestHistoryRecord("T1", true, 50));

        // Affected components: CompA (distance 0), CompB (distance 1) -> total affected = 2
        Map<String, ComponentImpact> impacted = Map.of(
                "CompA", new ComponentImpact("CompA", true, 0, List.of("CompA")),
                "CompB", new ComponentImpact("CompB", false, 1, List.of("CompA", "CompB"))
        );

        List<TestCandidate> scored = scorer.scoreCandidates(
                Set.of("T1"),
                impacted,
                coverage,
                history,
                50L
        );

        assertEquals(1, scored.size());
        TestCandidate cand = scored.getFirst();

        // 1. ImpactRelevance = 1 / (1 + 0) = 1.0
        assertEquals(1.0, cand.getImpactRelevance(), 1e-6);

        // 2. FailureSignal = 2 / 5 = 0.40
        assertEquals(0.40, cand.getHistoricalFailureSignal(), 1e-6);

        // 3. ChangeCoverage = 1 / 2 = 0.50
        assertEquals(0.50, cand.getChangeCoverage(), 1e-6);

        // 4. UncertaintyAdjustment = 1 / sqrt(3 + 1) = 1 / 2 = 0.50
        assertEquals(0.50, cand.getUncertaintyAdjustment(), 1e-6);

        // Composite: 0.35*1.0 + 0.25*0.40 + 0.25*0.50 + 0.15*0.50
        // = 0.35 + 0.10 + 0.125 + 0.075 = 0.6500
        assertEquals(0.6500, cand.getCompositeRiskScore(), 1e-6);
    }

    @Test
    @DisplayName("Should handle missing history safely with uninformative prior and maximum uncertainty")
    public void testMissingHistoryHandling() {
        RiskWeights weights = RiskWeights.defaultWeights();
        RiskScorer scorer = new RiskScorer(weights);

        CoverageRepository coverage = new CoverageRepository();
        coverage.addCoverage("NewTest", "CompA", CoverageSource.MEASURED);

        HistoryStore emptyHistory = new HistoryStore();
        Map<String, ComponentImpact> impacted = Map.of(
                "CompA", new ComponentImpact("CompA", true, 0, List.of("CompA"))
        );

        List<TestCandidate> scored = scorer.scoreCandidates(
                Set.of("NewTest"),
                impacted,
                coverage,
                emptyHistory,
                100L
        );

        assertEquals(1, scored.size());
        TestCandidate cand = scored.getFirst();

        // Uninformative prior is 0.5
        assertEquals(0.5, cand.getHistoricalFailureSignal(), 1e-6);
        // Uncertainty for 0 runs: 1 / sqrt(0 + 1) = 1.0
        assertEquals(1.0, cand.getUncertaintyAdjustment(), 1e-6);
        // Should not crash and composite score must be strictly between 0 and 1
        assertTrue(cand.getCompositeRiskScore() > 0.0 && cand.getCompositeRiskScore() <= 1.0);
    }
}
