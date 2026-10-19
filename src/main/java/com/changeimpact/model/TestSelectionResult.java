package com.changeimpact.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Result of test selection execution under a particular strategy and budget constraint.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TestSelectionResult {
    private final String strategyName;
    private final List<TestCandidate> selectedTests;
    private final List<TestCandidate> candidateTests;
    private final List<SelectionExplanation> explanations;
    private final long totalBudgetMs;
    private final long usedBudgetMs;
    private final long fullSuiteDurationMs;
    private final Set<String> coveredAffectedComponents;
    private final Set<String> totalAffectedComponents;
    private final Set<String> detectedFaults;
    private final Set<String> totalFaults;
    private final long analysisLatencyMs;

    @JsonCreator
    public TestSelectionResult(
            @JsonProperty("strategyName") String strategyName,
            @JsonProperty("selectedTests") List<TestCandidate> selectedTests,
            @JsonProperty("candidateTests") List<TestCandidate> candidateTests,
            @JsonProperty("explanations") List<SelectionExplanation> explanations,
            @JsonProperty("totalBudgetMs") long totalBudgetMs,
            @JsonProperty("usedBudgetMs") long usedBudgetMs,
            @JsonProperty("fullSuiteDurationMs") long fullSuiteDurationMs,
            @JsonProperty("coveredAffectedComponents") Set<String> coveredAffectedComponents,
            @JsonProperty("totalAffectedComponents") Set<String> totalAffectedComponents,
            @JsonProperty("detectedFaults") Set<String> detectedFaults,
            @JsonProperty("totalFaults") Set<String> totalFaults,
            @JsonProperty("analysisLatencyMs") long analysisLatencyMs) {
        this.strategyName = Objects.requireNonNull(strategyName, "strategyName cannot be null");
        this.selectedTests = selectedTests != null ? List.copyOf(selectedTests) : Collections.emptyList();
        this.candidateTests = candidateTests != null ? List.copyOf(candidateTests) : Collections.emptyList();
        this.explanations = explanations != null ? List.copyOf(explanations) : Collections.emptyList();
        this.totalBudgetMs = totalBudgetMs;
        this.usedBudgetMs = usedBudgetMs;
        this.fullSuiteDurationMs = fullSuiteDurationMs;
        this.coveredAffectedComponents = coveredAffectedComponents != null ? Set.copyOf(coveredAffectedComponents) : Collections.emptySet();
        this.totalAffectedComponents = totalAffectedComponents != null ? Set.copyOf(totalAffectedComponents) : Collections.emptySet();
        this.detectedFaults = detectedFaults != null ? Set.copyOf(detectedFaults) : Collections.emptySet();
        this.totalFaults = totalFaults != null ? Set.copyOf(totalFaults) : Collections.emptySet();
        this.analysisLatencyMs = analysisLatencyMs;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public List<TestCandidate> getSelectedTests() {
        return selectedTests;
    }

    public List<TestCandidate> getCandidateTests() {
        return candidateTests;
    }

    public List<SelectionExplanation> getExplanations() {
        return explanations;
    }

    public long getTotalBudgetMs() {
        return totalBudgetMs;
    }

    public long getUsedBudgetMs() {
        return usedBudgetMs;
    }

    public long getFullSuiteDurationMs() {
        return fullSuiteDurationMs;
    }

    public Set<String> getCoveredAffectedComponents() {
        return coveredAffectedComponents;
    }

    public Set<String> getTotalAffectedComponents() {
        return totalAffectedComponents;
    }

    public Set<String> getDetectedFaults() {
        return detectedFaults;
    }

    public Set<String> getTotalFaults() {
        return totalFaults;
    }

    public long getAnalysisLatencyMs() {
        return analysisLatencyMs;
    }

    public double getAffectedCoverageRatio() {
        if (totalAffectedComponents.isEmpty()) return 1.0;
        return (double) coveredAffectedComponents.size() / totalAffectedComponents.size();
    }

    public double getFaultDetectionRate() {
        if (totalFaults.isEmpty()) return 1.0;
        return (double) detectedFaults.size() / totalFaults.size();
    }

    public double getSelectionRatio(int fullSuiteTestCount) {
        if (fullSuiteTestCount <= 0) return 0.0;
        return (double) selectedTests.size() / fullSuiteTestCount;
    }

    public double getTimeReductionRatio() {
        if (fullSuiteDurationMs <= 0) return 0.0;
        return Math.max(0.0, 1.0 - ((double) usedBudgetMs / fullSuiteDurationMs));
    }
}
