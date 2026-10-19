package com.changeimpact.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Directed edge in the dependency graph indicating that `fromNodeId` depends on or references `toNodeId`.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DependencyEdge {
    private final String fromNodeId;
    private final String toNodeId;
    private final EdgeType type;
    private final double weight;
    private final Map<String, Object> metadata;

    @JsonCreator
    public DependencyEdge(
            @JsonProperty("fromNodeId") String fromNodeId,
            @JsonProperty("toNodeId") String toNodeId,
            @JsonProperty("type") EdgeType type,
            @JsonProperty("weight") Double weight,
            @JsonProperty("metadata") Map<String, Object> metadata) {
        this.fromNodeId = Objects.requireNonNull(fromNodeId, "fromNodeId cannot be null").trim();
        this.toNodeId = Objects.requireNonNull(toNodeId, "toNodeId cannot be null").trim();
        this.type = type != null ? type : EdgeType.DEPENDS_ON;
        this.weight = weight != null ? weight : 1.0;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public DependencyEdge(String fromNodeId, String toNodeId, EdgeType type) {
        this(fromNodeId, toNodeId, type, 1.0, Collections.emptyMap());
    }

    public DependencyEdge(String fromNodeId, String toNodeId) {
        this(fromNodeId, toNodeId, EdgeType.DEPENDS_ON, 1.0, Collections.emptyMap());
    }

    public String getFromNodeId() {
        return fromNodeId;
    }

    public String getToNodeId() {
        return toNodeId;
    }

    public EdgeType getType() {
        return type;
    }

    public double getWeight() {
        return weight;
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DependencyEdge that = (DependencyEdge) o;
        return Objects.equals(fromNodeId, that.fromNodeId) &&
                Objects.equals(toNodeId, that.toNodeId) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromNodeId, toNodeId, type);
    }

    @Override
    public String toString() {
        return fromNodeId + " --[" + type + "]-> " + toNodeId;
    }
}
