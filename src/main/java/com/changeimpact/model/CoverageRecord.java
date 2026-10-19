package com.changeimpact.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Coverage association between a test and a component.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CoverageRecord {
    private final String testId;
    private final String componentId;
    private final int linesCovered;
    private final int branchesCovered;
    private final CoverageSource source;

    @JsonCreator
    public CoverageRecord(
            @JsonProperty("testId") String testId,
            @JsonProperty("componentId") String componentId,
            @JsonProperty("linesCovered") int linesCovered,
            @JsonProperty("branchesCovered") int branchesCovered,
            @JsonProperty("source") CoverageSource source) {
        this.testId = Objects.requireNonNull(testId, "testId cannot be null").trim();
        this.componentId = Objects.requireNonNull(componentId, "componentId cannot be null").trim();
        this.linesCovered = Math.max(0, linesCovered);
        this.branchesCovered = Math.max(0, branchesCovered);
        this.source = source != null ? source : CoverageSource.SYNTHETIC;
    }

    public CoverageRecord(String testId, String componentId, CoverageSource source) {
        this(testId, componentId, 1, 1, source);
    }

    public String getTestId() {
        return testId;
    }

    public String getComponentId() {
        return componentId;
    }

    public int getLinesCovered() {
        return linesCovered;
    }

    public int getBranchesCovered() {
        return branchesCovered;
    }

    public CoverageSource getSource() {
        return source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CoverageRecord that = (CoverageRecord) o;
        return Objects.equals(testId, that.testId) && Objects.equals(componentId, that.componentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testId, componentId);
    }

    @Override
    public String toString() {
        return testId + " covers " + componentId + " (" + source + ")";
    }
}
