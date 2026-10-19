package com.changeimpact.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Detailed, human-readable and machine-verifiable explanation of why a test was selected or skipped.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SelectionExplanation {
    private final String testId;
    private final SelectionStatus status;
    private final int selectionOrder;
    private final double marginalGain;
    private final long costMs;
    private final double marginalEfficiency;
    private final Set<String> newlyCoveredComponents;
    private final long cumulativeTimeMs;
    private final String rationale;

    @JsonCreator
    public SelectionExplanation(
            @JsonProperty("testId") String testId,
            @JsonProperty("status") SelectionStatus status,
            @JsonProperty("selectionOrder") int selectionOrder,
            @JsonProperty("marginalGain") double marginalGain,
            @JsonProperty("costMs") long costMs,
            @JsonProperty("marginalEfficiency") double marginalEfficiency,
            @JsonProperty("newlyCoveredComponents") Set<String> newlyCoveredComponents,
            @JsonProperty("cumulativeTimeMs") long cumulativeTimeMs,
            @JsonProperty("rationale") String rationale) {
        this.testId = Objects.requireNonNull(testId, "testId cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.selectionOrder = selectionOrder;
        this.marginalGain = marginalGain;
        this.costMs = costMs;
        this.marginalEfficiency = marginalEfficiency;
        this.newlyCoveredComponents = newlyCoveredComponents != null ? Set.copyOf(newlyCoveredComponents) : Collections.emptySet();
        this.cumulativeTimeMs = cumulativeTimeMs;
        this.rationale = rationale != null ? rationale : "";
    }

    public String getTestId() {
        return testId;
    }

    public SelectionStatus getStatus() {
        return status;
    }

    public int getSelectionOrder() {
        return selectionOrder;
    }

    public double getMarginalGain() {
        return marginalGain;
    }

    public long getCostMs() {
        return costMs;
    }

    public double getMarginalEfficiency() {
        return marginalEfficiency;
    }

    public Set<String> getNewlyCoveredComponents() {
        return newlyCoveredComponents;
    }

    public long getCumulativeTimeMs() {
        return cumulativeTimeMs;
    }

    public String getRationale() {
        return rationale;
    }

    @Override
    public String toString() {
        return "[" + status + "] " + testId + " (order=" + selectionOrder + ", cost=" + costMs + "ms, marginalGain=" +
                String.format("%.4f", marginalGain) + ", newlyCovered=" + newlyCoveredComponents + "): " + rationale;
    }
}
