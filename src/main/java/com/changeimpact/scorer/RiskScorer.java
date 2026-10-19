package com.changeimpact.scorer;

import com.changeimpact.coverage.CoverageRepository;
import com.changeimpact.history.HistoryStore;
import com.changeimpact.model.ComponentImpact;
import com.changeimpact.model.TestCandidate;

import java.util.*;

/**
 * Computes risk scores and feature values for candidate tests.
 * All features are normalized in [0.0, 1.0].
 * Missing historical data is handled gracefully using uninformative priors.
 */
public class RiskScorer {

    private final RiskWeights weights;
    private final double recencyDecay;
    private final boolean useRecencyWeighting;

    public RiskScorer(RiskWeights weights, double recencyDecay, boolean useRecencyWeighting) {
        this.weights = weights != null ? weights : RiskWeights.defaultWeights();
        this.recencyDecay = recencyDecay > 0 && recencyDecay <= 1.0 ? recencyDecay : 0.85;
        this.useRecencyWeighting = useRecencyWeighting;
    }

    public RiskScorer(RiskWeights weights) {
        this(weights, 0.85, true);
    }

    public RiskScorer() {
        this(RiskWeights.defaultWeights(), 0.85, true);
    }

    public RiskWeights getWeights() {
        return weights;
    }

    /**
     * Score all candidate tests based on affected components, coverage, and history.
     *
     * @param candidateTestIds   tests to evaluate
     * @param impactedComponents affected components with distances and paths
     * @param coverage           test-to-component coverage mappings
     * @param history            historical test execution data
     * @param defaultDurationMs  fallback duration if history is missing
     * @return list of fully scored TestCandidates sorted by priority
     */
    public List<TestCandidate> scoreCandidates(
            Set<String> candidateTestIds,
            Map<String, ComponentImpact> impactedComponents,
            CoverageRepository coverage,
            HistoryStore history,
            long defaultDurationMs) {

        List<TestCandidate> scoredList = new ArrayList<>();
        if (candidateTestIds == null || candidateTestIds.isEmpty()) {
            return scoredList;
        }

        Set<String> allAffected = impactedComponents != null ? impactedComponents.keySet() : Collections.emptySet();

        for (String testId : candidateTestIds) {
            Set<String> covered = coverage != null ? coverage.getComponentsExercisedBy(testId) : Collections.emptySet();
            Set<String> affectedCovered = new LinkedHashSet<>(covered);
            affectedCovered.retainAll(allAffected);

            // 1. Impact Relevance: proximity to change origin
            double impactRelevance = computeImpactRelevance(affectedCovered, impactedComponents);

            // 2. Historical Failure Signal
            double failureSignal = computeFailureSignal(testId, history);

            // 3. Change Coverage breadth
            double changeCoverage = computeChangeCoverage(affectedCovered, allAffected);

            // 4. Uncertainty Adjustment
            double uncertainty = computeUncertaintyAdjustment(testId, history);

            // Composite score
            double compositeRisk = (weights.getW1ImpactRelevance() * impactRelevance)
                    + (weights.getW2FailureSignal() * failureSignal)
                    + (weights.getW3ChangeCoverage() * changeCoverage)
                    + (weights.getW4UncertaintyAdjustment() * uncertainty);

            // Duration estimate
            long durationMs = history != null
                    ? history.getMeanDurationMs(testId, defaultDurationMs)
                    : defaultDurationMs;

            TestCandidate candidate = new TestCandidate(
                    testId,
                    testId,
                    null,
                    durationMs,
                    covered,
                    affectedCovered,
                    impactRelevance,
                    failureSignal,
                    changeCoverage,
                    uncertainty,
                    compositeRisk
            );
            scoredList.add(candidate);
        }

        // Deterministic stable sorting:
        // 1. Composite risk score descending
        // 2. Execution cost ascending
        // 3. Test ID lexicographical ascending
        scoredList.sort(Comparator
                .comparingDouble(TestCandidate::getCompositeRiskScore).reversed()
                .thenComparingLong(TestCandidate::getCostMs)
                .thenComparing(TestCandidate::getTestId));

        return scoredList;
    }

    public double computeImpactRelevance(Set<String> affectedCovered, Map<String, ComponentImpact> impactedComponents) {
        if (affectedCovered.isEmpty() || impactedComponents == null || impactedComponents.isEmpty()) {
            return 0.0;
        }
        int minDistance = Integer.MAX_VALUE;
        for (String compId : affectedCovered) {
            ComponentImpact impact = impactedComponents.get(compId);
            if (impact != null) {
                minDistance = Math.min(minDistance, impact.getDistance());
            }
        }
        if (minDistance == Integer.MAX_VALUE) {
            return 0.0;
        }
        // Normalized: distance 0 -> 1.0, distance 1 -> 0.5, distance 2 -> 0.333...
        return 1.0 / (1.0 + minDistance);
    }

    public double computeFailureSignal(String testId, HistoryStore history) {
        if (history == null || !history.hasHistory(testId)) {
            // Uninformative prior acknowledging uncertainty
            return 0.5;
        }
        if (useRecencyWeighting) {
            return history.getRecencyWeightedFailureSignal(testId, recencyDecay);
        } else {
            return history.getSmoothedFailureRate(testId);
        }
    }

    public double computeChangeCoverage(Set<String> affectedCovered, Set<String> allAffected) {
        if (allAffected == null || allAffected.isEmpty()) {
            return 0.0;
        }
        return (double) affectedCovered.size() / allAffected.size();
    }

    public double computeUncertaintyAdjustment(String testId, HistoryStore history) {
        if (history == null) {
            return 1.0;
        }
        int runs = history.getTotalRuns(testId);
        return 1.0 / Math.sqrt(runs + 1.0);
    }
}
