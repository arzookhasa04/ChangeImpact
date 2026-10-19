package com.changeimpact.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents an affected component and the dependency path from the change origin.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ComponentImpact {
    private final String componentId;
    private final boolean directChange;
    private final int distance;
    private final List<String> impactPath;

    @JsonCreator
    public ComponentImpact(
            @JsonProperty("componentId") String componentId,
            @JsonProperty("directChange") boolean directChange,
            @JsonProperty("distance") int distance,
            @JsonProperty("impactPath") List<String> impactPath) {
        this.componentId = Objects.requireNonNull(componentId, "componentId cannot be null");
        this.directChange = directChange;
        this.distance = distance;
        this.impactPath = impactPath != null ? List.copyOf(impactPath) : Collections.emptyList();
    }

    public String getComponentId() {
        return componentId;
    }

    public boolean isDirectChange() {
        return directChange;
    }

    public int getDistance() {
        return distance;
    }

    public List<String> getImpactPath() {
        return impactPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentImpact that = (ComponentImpact) o;
        return directChange == that.directChange &&
                distance == that.distance &&
                Objects.equals(componentId, that.componentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentId, directChange, distance);
    }

    @Override
    public String toString() {
        return "ComponentImpact{" +
                "componentId='" + componentId + '\'' +
                ", direct=" + directChange +
                ", distance=" + distance +
                ", path=" + String.join(" -> ", impactPath) +
                '}';
    }
}
