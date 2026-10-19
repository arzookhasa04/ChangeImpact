package com.changeimpact.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * A test candidate identified as relevant to a change, holding scoring features.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TestCandidate {
    private final String testId;
    private final String testName;
    private final String filePath;
    private final long costMs;
    private final Set<String> coveredComponents;
    private final Set<String> affectedComponents;
    private final double impactRelevance;
    private final double historicalFailureSignal;
    private final double changeCoverage;
    private final double uncertaintyAdjustment;
    private final double compositeRiskScore;

    @JsonCreator
    public TestCandidate(
            @JsonProperty("testId") String testId,
            @JsonProperty("testName") String testName,
            @JsonProperty("filePath") String filePath,
            @JsonProperty("costMs") long costMs,
            @JsonProperty("coveredComponents") Set<String> coveredComponents,
            @JsonProperty("affectedComponents") Set<String> affectedComponents,
            @JsonProperty("impactRelevance") double impactRelevance,
            @JsonProperty("historicalFailureSignal") double historicalFailureSignal,
            @JsonProperty("changeCoverage") double changeCoverage,
            @JsonProperty("uncertaintyAdjustment") double uncertaintyAdjustment,
            @JsonProperty("compositeRiskScore") double compositeRiskScore) {
        this.testId = Objects.requireNonNull(testId, "testId cannot be null");
        this.testName = testName != null ? testName : testId;
        this.filePath = filePath;
        this.costMs = Math.max(1, costMs); // Guard against 0 or negative cost
        this.coveredComponents = coveredComponents != null ? Set.copyOf(coveredComponents) : Collections.emptySet();
        this.affectedComponents = affectedComponents != null ? Set.copyOf(affectedComponents) : Collections.emptySet();
        this.impactRelevance = impactRelevance;
        this.historicalFailureSignal = historicalFailureSignal;
        this.changeCoverage = changeCoverage;
        this.uncertaintyAdjustment = uncertaintyAdjustment;
        this.compositeRiskScore = compositeRiskScore;
    }

    public TestCandidate withScores(
            double impactRelevance,
            double historicalFailureSignal,
            double changeCoverage,
            double uncertaintyAdjustment,
            double compositeRiskScore) {
        return new TestCandidate(
                this.testId,
                this.testName,
                this.filePath,
                this.costMs,
                this.coveredComponents,
                this.affectedComponents,
                impactRelevance,
                historicalFailureSignal,
                changeCoverage,
                uncertaintyAdjustment,
                compositeRiskScore
        );
    }

    public String getTestId() {
        return testId;
    }

    public String getTestName() {
        return testName;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getCostMs() {
        return costMs;
    }

    public Set<String> getCoveredComponents() {
        return coveredComponents;
    }

    public Set<String> getAffectedComponents() {
        return affectedComponents;
    }

    public double getImpactRelevance() {
        return impactRelevance;
    }

    public double getHistoricalFailureSignal() {
        return historicalFailureSignal;
    }

    public double getChangeCoverage() {
        return changeCoverage;
    }

    public double getUncertaintyAdjustment() {
        return uncertaintyAdjustment;
    }

    public double getCompositeRiskScore() {
        return compositeRiskScore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestCandidate that = (TestCandidate) o;
        return Objects.equals(testId, that.testId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testId);
    }

    @Override
    public String toString() {
        return "TestCandidate{" +
                "id='" + testId + '\'' +
                ", cost=" + costMs + "ms" +
                ", risk=" + String.format("%.4f", compositeRiskScore) +
                ", affectedCovered=" + affectedComponents.size() +
                '}';
    }
}
