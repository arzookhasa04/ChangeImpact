package com.changeimpact.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * An injected fault in a component with known ground-truth detectable tests.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class InjectedFault {
    private final String faultId;
    private final String componentId;
    private final String description;
    private final Set<String> detectableByTests;

    @JsonCreator
    public InjectedFault(
            @JsonProperty("faultId") String faultId,
            @JsonProperty("componentId") String componentId,
            @JsonProperty("description") String description,
            @JsonProperty("detectableByTests") Set<String> detectableByTests) {
        this.faultId = Objects.requireNonNull(faultId, "faultId cannot be null").trim();
        this.componentId = Objects.requireNonNull(componentId, "componentId cannot be null").trim();
        this.description = description != null ? description.trim() : "";
        this.detectableByTests = detectableByTests != null ? Set.copyOf(detectableByTests) : Collections.emptySet();
    }

    public String getFaultId() {
        return faultId;
    }

    public String getComponentId() {
        return componentId;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getDetectableByTests() {
        return detectableByTests;
    }

    public boolean isDetectedByAny(Set<String> executedTests) {
        if (executedTests == null || executedTests.isEmpty()) return false;
        for (String testId : detectableByTests) {
            if (executedTests.contains(testId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InjectedFault that = (InjectedFault) o;
        return Objects.equals(faultId, that.faultId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(faultId);
    }

    @Override
    public String toString() {
        return "Fault[" + faultId + " in " + componentId + ", detectableBy=" + detectableByTests + "]";
    }
}
