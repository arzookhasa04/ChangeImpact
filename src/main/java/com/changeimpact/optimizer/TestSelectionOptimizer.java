package com.changeimpact.optimizer;

import com.changeimpact.model.*;
import com.changeimpact.scorer.RiskWeights;

import java.util.*;

/**
 * Optimizes test selection under configurable execution-time budgets and strategies.
 * Provides deterministic greedy marginal coverage per cost, deduplicating component coverage.
 */
public class TestSelectionOptimizer {

    public TestSelectionOptimizer() {
    }

    /**
     * Select tests according to the given strategy and execution budget.
     *
     * @param strategy            selection strategy (FULL_SUITE, DEPENDENCY_BASED, RISK_AWARE_BUDGETED, etc.)
     * @param candidates          all candidate tests scored by RiskScorer
     * @param allSuiteTests       all known tests in the full suite (for FULL_SUITE and full baseline time)
     * @param allAffected         set of all affected component IDs
     * @param timeBudgetMs        maximum execution budget in milliseconds (hard constraint)
     * @param weights             risk weights for marginal recalculations
     * @param groundTruthFaults   optional ground-truth injected faults for evaluation
     * @return explainable TestSelectionResult
     */
    public TestSelectionResult selectTests(
            TestSelectionStrategy strategy,
            List<TestCandidate> candidates,
            List<TestCandidate> allSuiteTests,
            Set<String> allAffected,
            long timeBudgetMs,
            RiskWeights weights,
            List<InjectedFault> groundTruthFaults) {

        long startLatency = System.currentTimeMillis();
        long fullSuiteDurationMs = allSuiteTests.stream().mapToLong(TestCandidate::getCostMs).sum();
        Set<String> affectedSet = allAffected != null ? allAffected : Collections.emptySet();

        switch (strategy) {
            case FULL_SUITE:
                return selectFullSuite(allSuiteTests, candidates, affectedSet, fullSuiteDurationMs, groundTruthFaults, startLatency);
            case DEPENDENCY_BASED:
                return selectDependencyBased(candidates, allSuiteTests, affectedSet, fullSuiteDurationMs, groundTruthFaults, startLatency);
            case GREEDY_COST_ONLY:
                return selectGreedyCostOnly(candidates, allSuiteTests, affectedSet, timeBudgetMs, fullSuiteDurationMs, groundTruthFaults, startLatency);
            case GREEDY_FAILURE_ONLY:
                return selectGreedyFailureOnly(candidates, allSuiteTests, affectedSet, timeBudgetMs, fullSuiteDurationMs, groundTruthFaults, startLatency);
            case RISK_AWARE_BUDGETED:
            default:
                return selectRiskAwareBudgeted(candidates, allSuiteTests, affectedSet, timeBudgetMs, weights, fullSuiteDurationMs, groundTruthFaults, startLatency);
        }
    }

    private TestSelectionResult selectFullSuite(
            List<TestCandidate> allSuiteTests,
            List<TestCandidate> candidates,
            Set<String> allAffected,
            long fullSuiteDurationMs,
            List<InjectedFault> groundTruthFaults,
            long startLatency) {

        List<SelectionExplanation> explanations = new ArrayList<>();
        Set<String> coveredAffected = new LinkedHashSet<>();
        long cumulativeTime = 0;
        int order = 1;

        for (TestCandidate t : allSuiteTests) {
            cumulativeTime += t.getCostMs();
            Set<String> testAffected = new LinkedHashSet<>(t.getCoveredComponents());
            testAffected.retainAll(allAffected);

            Set<String> newlyCovered = new LinkedHashSet<>(testAffected);
            newlyCovered.removeAll(coveredAffected);
            coveredAffected.addAll(testAffected);

            explanations.add(new SelectionExplanation(
                    t.getTestId(),
                    SelectionStatus.SELECTED,
                    order++,
                    t.getCompositeRiskScore(),
                    t.getCostMs(),
                    t.getCompositeRiskScore() / Math.max(1, t.getCostMs()),
                    newlyCovered,
                    cumulativeTime,
                    "Selected as part of full suite baseline execution."
            ));
        }

        Set<String> detectedFaults = computeDetectedFaults(allSuiteTests, groundTruthFaults);
        Set<String> allFaultIds = groundTruthFaults != null ? extractFaultIds(groundTruthFaults) : Collections.emptySet();

        return new TestSelectionResult(
                TestSelectionStrategy.FULL_SUITE.name(),
                allSuiteTests,
                candidates,
                explanations,
                fullSuiteDurationMs,
                cumulativeTime,
                fullSuiteDurationMs,
                coveredAffected,
                allAffected,
                detectedFaults,
                allFaultIds,
                System.currentTimeMillis() - startLatency
        );
    }

    private TestSelectionResult selectDependencyBased(
            List<TestCandidate> candidates,
            List<TestCandidate> allSuiteTests,
            Set<String> allAffected,
            long fullSuiteDurationMs,
            List<InjectedFault> groundTruthFaults,
            long startLatency) {

        List<SelectionExplanation> explanations = new ArrayList<>();
        Set<String> coveredAffected = new LinkedHashSet<>();
        long cumulativeTime = 0;
        int order = 1;

        for (TestCandidate t : candidates) {
            cumulativeTime += t.getCostMs();
            Set<String> newlyCovered = new LinkedHashSet<>(t.getAffectedComponents());
            newlyCovered.removeAll(coveredAffected);
            coveredAffected.addAll(t.getAffectedComponents());

            explanations.add(new SelectionExplanation(
                    t.getTestId(),
                    SelectionStatus.SELECTED,
                    order++,
                    t.getCompositeRiskScore(),
                    t.getCostMs(),
                    t.getCompositeRiskScore() / Math.max(1, t.getCostMs()),
                    newlyCovered,
                    cumulativeTime,
                    "Selected by dependency analysis (exercises " + t.getAffectedComponents().size() + " affected component(s))."
            ));
        }

        Set<String> detectedFaults = computeDetectedFaults(candidates, groundTruthFaults);
        Set<String> allFaultIds = groundTruthFaults != null ? extractFaultIds(groundTruthFaults) : Collections.emptySet();

        return new TestSelectionResult(
                TestSelectionStrategy.DEPENDENCY_BASED.name(),
                candidates,
                candidates,
                explanations,
                cumulativeTime, // Unbudgeted: budget is execution time needed
                cumulativeTime,
                fullSuiteDurationMs,
                coveredAffected,
                allAffected,
                detectedFaults,
                allFaultIds,
                System.currentTimeMillis() - startLatency
        );
    }

    private TestSelectionResult selectGreedyCostOnly(
            List<TestCandidate> candidates,
            List<TestCandidate> allSuiteTests,
            Set<String> allAffected,
            long timeBudgetMs,
            long fullSuiteDurationMs,
            List<InjectedFault> groundTruthFaults,
            long startLatency) {

        List<TestCandidate> sortedByCost = new ArrayList<>(candidates);
        sortedByCost.sort(Comparator.comparingLong(TestCandidate::getCostMs).thenComparing(TestCandidate::getTestId));

        List<TestCandidate> selected = new ArrayList<>();
        List<SelectionExplanation> explanations = new ArrayList<>();
        Set<String> coveredAffected = new LinkedHashSet<>();
        long remainingBudget = timeBudgetMs;
        long cumulativeTime = 0;
        int order = 1;

        for (TestCandidate t : sortedByCost) {
            if (t.getCostMs() <= remainingBudget) {
                selected.add(t);
                remainingBudget -= t.getCostMs();
                cumulativeTime += t.getCostMs();

                Set<String> newlyCovered = new LinkedHashSet<>(t.getAffectedComponents());
                newlyCovered.removeAll(coveredAffected);
                coveredAffected.addAll(t.getAffectedComponents());

                explanations.add(new SelectionExplanation(
                        t.getTestId(),
                        SelectionStatus.SELECTED,
                        order++,
                        t.getCompositeRiskScore(),
                        t.getCostMs(),
                        1.0 / t.getCostMs(),
                        newlyCovered,
                        cumulativeTime,
                        "Selected by Cost-Only baseline (cost: " + t.getCostMs() + "ms)."
                ));
            } else {
                explanations.add(new SelectionExplanation(
                        t.getTestId(),
                        SelectionStatus.SKIPPED_BUDGET_EXCEEDED,
                        -1,
                        t.getCompositeRiskScore(),
                        t.getCostMs(),
                        1.0 / t.getCostMs(),
                        Collections.emptySet(),
                        cumulativeTime,
                        "Skipped: cost " + t.getCostMs() + "ms exceeds remaining budget " + remainingBudget + "ms."
                ));
            }
        }

        Set<String> detectedFaults = computeDetectedFaults(selected, groundTruthFaults);
        Set<String> allFaultIds = groundTruthFaults != null ? extractFaultIds(groundTruthFaults) : Collections.emptySet();

        return new TestSelectionResult(
                TestSelectionStrategy.GREEDY_COST_ONLY.name(),
                selected,
                candidates,
                explanations,
                timeBudgetMs,
                cumulativeTime,
                fullSuiteDurationMs,
                coveredAffected,
                allAffected,
                detectedFaults,
                allFaultIds,
                System.currentTimeMillis() - startLatency
        );
    }

    private TestSelectionResult selectGreedyFailureOnly(
            List<TestCandidate> candidates,
            List<TestCandidate> allSuiteTests,
            Set<String> allAffected,
            long timeBudgetMs,
            long fullSuiteDurationMs,
            List<InjectedFault> groundTruthFaults,
            long startLatency) {

        List<TestCandidate> sortedByFailure = new ArrayList<>(candidates);
        sortedByFailure.sort(Comparator.comparingDouble(TestCandidate::getHistoricalFailureSignal).reversed()
                .thenComparingLong(TestCandidate::getCostMs)
                .thenComparing(TestCandidate::getTestId));

        List<TestCandidate> selected = new ArrayList<>();
        List<SelectionExplanation> explanations = new ArrayList<>();
        Set<String> coveredAffected = new LinkedHashSet<>();
        long remainingBudget = timeBudgetMs;
        long cumulativeTime = 0;
        int order = 1;

        for (TestCandidate t : sortedByFailure) {
            if (t.getCostMs() <= remainingBudget) {
                selected.add(t);
                remainingBudget -= t.getCostMs();
                cumulativeTime += t.getCostMs();

                Set<String> newlyCovered = new LinkedHashSet<>(t.getAffectedComponents());
                newlyCovered.removeAll(coveredAffected);
                coveredAffected.addAll(t.getAffectedComponents());

                explanations.add(new SelectionExplanation(
                        t.getTestId(),
                        SelectionStatus.SELECTED,
                        order++,
                        t.getHistoricalFailureSignal(),
                        t.getCostMs(),
                        t.getHistoricalFailureSignal() / t.getCostMs(),
                        newlyCovered,
                        cumulativeTime,
                        "Selected by Failure-Only baseline (failure signal: " + String.format("%.2f", t.getHistoricalFailureSignal()) + ")."
                ));
            } else {
                explanations.add(new SelectionExplanation(
                        t.getTestId(),
                        SelectionStatus.SKIPPED_BUDGET_EXCEEDED,
                        -1,
                        t.getHistoricalFailureSignal(),
                        t.getCostMs(),
                        t.getHistoricalFailureSignal() / t.getCostMs(),
                        Collections.emptySet(),
                        cumulativeTime,
                        "Skipped: cost " + t.getCostMs() + "ms exceeds remaining budget " + remainingBudget + "ms."
                ));
            }
        }

        Set<String> detectedFaults = computeDetectedFaults(selected, groundTruthFaults);
        Set<String> allFaultIds = groundTruthFaults != null ? extractFaultIds(groundTruthFaults) : Collections.emptySet();

        return new TestSelectionResult(
                TestSelectionStrategy.GREEDY_FAILURE_ONLY.name(),
                selected,
                candidates,
                explanations,
                timeBudgetMs,
                cumulativeTime,
                fullSuiteDurationMs,
                coveredAffected,
                allAffected,
                detectedFaults,
                allFaultIds,
                System.currentTimeMillis() - startLatency
        );
    }

    /**
     * Risk-aware budget-constrained selection.
     * Evaluates marginal gain per cost dynamically, deduplicating coverage over already-covered components.
     */
    private TestSelectionResult selectRiskAwareBudgeted(
            List<TestCandidate> candidates,
            List<TestCandidate> allSuiteTests,
            Set<String> allAffected,
            long timeBudgetMs,
            RiskWeights weights,
            long fullSuiteDurationMs,
            List<InjectedFault> groundTruthFaults,
            long startLatency) {

        RiskWeights rw = weights != null ? weights : RiskWeights.defaultWeights();
        Set<String> coveredAffected = new LinkedHashSet<>();
        List<TestCandidate> remainingPool = new ArrayList<>(candidates);
        List<TestCandidate> selected = new ArrayList<>();
        List<SelectionExplanation> explanations = new ArrayList<>();

        long remainingBudget = timeBudgetMs;
        long cumulativeTime = 0;
        int order = 1;

        while (!remainingPool.isEmpty()) {
            // Find eligible candidate with highest marginal gain per unit cost
            TestCandidate bestCandidate = null;
            double bestEfficiency = -1.0;
            double bestMarginalGain = 0.0;
            Set<String> bestNewlyCovered = Collections.emptySet();

            for (TestCandidate t : remainingPool) {
                // Hard budget check
                if (t.getCostMs() > remainingBudget) {
                    continue;
                }

                // Compute newly covered affected components
                Set<String> newlyCovered = new LinkedHashSet<>(t.getAffectedComponents());
                newlyCovered.removeAll(coveredAffected);

                double marginalScore;
                if (newlyCovered.isEmpty()) {
                    // Prevent duplicate credit: if no new affected components are covered, marginal gain is 0
                    marginalScore = 0.0;
                } else {
                    double marginalCoverage = allAffected.isEmpty() ? 0.0 : (double) newlyCovered.size() / allAffected.size();
                    double noveltyRatio = (double) newlyCovered.size() / Math.max(1, t.getAffectedComponents().size());

                    // Marginal score scales relevance, failure, and uncertainty by novelty
                    marginalScore = (rw.getW1ImpactRelevance() * t.getImpactRelevance() * noveltyRatio)
                            + (rw.getW2FailureSignal() * t.getHistoricalFailureSignal() * noveltyRatio)
                            + (rw.getW3ChangeCoverage() * marginalCoverage)
                            + (rw.getW4UncertaintyAdjustment() * t.getUncertaintyAdjustment() * noveltyRatio);
                }

                double efficiency = marginalScore / Math.max(1, t.getCostMs());

                // Deterministic comparison:
                // 1. Higher efficiency
                // 2. Higher marginal score
                // 3. Lower cost
                // 4. Test ID lexicographical
                boolean isBetter = false;
                if (bestCandidate == null) {
                    if (marginalScore > 0.0) {
                        isBetter = true;
                    }
                } else if (marginalScore > 0.0) {
                    if (Math.abs(efficiency - bestEfficiency) > 1e-9) {
                        isBetter = efficiency > bestEfficiency;
                    } else if (Math.abs(marginalScore - bestMarginalGain) > 1e-9) {
                        isBetter = marginalScore > bestMarginalGain;
                    } else if (t.getCostMs() != bestCandidate.getCostMs()) {
                        isBetter = t.getCostMs() < bestCandidate.getCostMs();
                    } else {
                        isBetter = t.getTestId().compareTo(bestCandidate.getTestId()) < 0;
                    }
                }

                if (isBetter) {
                    bestCandidate = t;
                    bestEfficiency = efficiency;
                    bestMarginalGain = marginalScore;
                    bestNewlyCovered = newlyCovered;
                }
            }

            if (bestCandidate == null || bestMarginalGain <= 0.0) {
                // No remaining candidate fits within remaining budget or adds new marginal coverage
                break;
            }

            // Select best candidate
            selected.add(bestCandidate);
            remainingPool.remove(bestCandidate);
            remainingBudget -= bestCandidate.getCostMs();
            cumulativeTime += bestCandidate.getCostMs();
            coveredAffected.addAll(bestNewlyCovered);

            explanations.add(new SelectionExplanation(
                    bestCandidate.getTestId(),
                    SelectionStatus.SELECTED,
                    order++,
                    bestMarginalGain,
                    bestCandidate.getCostMs(),
                    bestEfficiency,
                    bestNewlyCovered,
                    cumulativeTime,
                    String.format("Selected (marginalGain=%.4f, efficiency=%.6f, newlyCovered=%d, remainingBudget=%dms)",
                            bestMarginalGain, bestEfficiency, bestNewlyCovered.size(), remainingBudget)
            ));
        }

        // Record explanation for all skipped candidates
        for (TestCandidate t : remainingPool) {
            Set<String> newlyCovered = new LinkedHashSet<>(t.getAffectedComponents());
            newlyCovered.removeAll(coveredAffected);

            SelectionStatus status;
            String rationale;

            if (newlyCovered.isEmpty()) {
                status = SelectionStatus.SKIPPED_REDUNDANT;
                rationale = "All affected components covered by this test are already covered by previously selected tests.";
            } else if (t.getCostMs() > remainingBudget) {
                status = SelectionStatus.SKIPPED_BUDGET_EXCEEDED;
                rationale = String.format("Cost %dms exceeds remaining budget %dms.", t.getCostMs(), remainingBudget);
            } else {
                status = SelectionStatus.SKIPPED_ZERO_MARGINAL_GAIN;
                rationale = "Candidate marginal utility was lower than selected alternatives.";
            }

            explanations.add(new SelectionExplanation(
                    t.getTestId(),
                    status,
                    -1,
                    0.0,
                    t.getCostMs(),
                    0.0,
                    Collections.emptySet(),
                    cumulativeTime,
                    rationale
            ));
        }

        Set<String> detectedFaults = computeDetectedFaults(selected, groundTruthFaults);
        Set<String> allFaultIds = groundTruthFaults != null ? extractFaultIds(groundTruthFaults) : Collections.emptySet();

        return new TestSelectionResult(
                TestSelectionStrategy.RISK_AWARE_BUDGETED.name(),
                selected,
                candidates,
                explanations,
                timeBudgetMs,
                cumulativeTime,
                fullSuiteDurationMs,
                coveredAffected,
                allAffected,
                detectedFaults,
                allFaultIds,
                System.currentTimeMillis() - startLatency
        );
    }

    private Set<String> computeDetectedFaults(List<TestCandidate> selectedTests, List<InjectedFault> faults) {
        if (faults == null || faults.isEmpty() || selectedTests == null || selectedTests.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> selectedTestIds = new HashSet<>();
        for (TestCandidate t : selectedTests) {
            selectedTestIds.add(t.getTestId());
        }

        Set<String> detected = new LinkedHashSet<>();
        for (InjectedFault fault : faults) {
            if (fault.isDetectedByAny(selectedTestIds)) {
                detected.add(fault.getFaultId());
            }
        }
        return detected;
    }

    private Set<String> extractFaultIds(List<InjectedFault> faults) {
        Set<String> set = new LinkedHashSet<>();
        for (InjectedFault f : faults) {
            set.add(f.getFaultId());
        }
        return set;
    }
}
