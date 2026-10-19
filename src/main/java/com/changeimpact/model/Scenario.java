package com.changeimpact.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Benchmark scenario representing a code change and associated injected faults.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Scenario {
    private final String scenarioId;
    private final String name;
    private final String description;
    private final Set<String> changedComponents;
    private final List<InjectedFault> injectedFaults;

    @JsonCreator
    public Scenario(
            @JsonProperty("scenarioId") String scenarioId,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("changedComponents") Set<String> changedComponents,
            @JsonProperty("injectedFaults") List<InjectedFault> injectedFaults) {
        this.scenarioId = Objects.requireNonNull(scenarioId, "scenarioId cannot be null").trim();
        this.name = name != null ? name.trim() : this.scenarioId;
        this.description = description != null ? description.trim() : "";
        this.changedComponents = changedComponents != null ? Set.copyOf(changedComponents) : Collections.emptySet();
        this.injectedFaults = injectedFaults != null ? List.copyOf(injectedFaults) : Collections.emptyList();
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getChangedComponents() {
        return changedComponents;
    }

    public List<InjectedFault> getInjectedFaults() {
        return injectedFaults;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Scenario scenario = (Scenario) o;
        return Objects.equals(scenarioId, scenario.scenarioId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scenarioId);
    }

    @Override
    public String toString() {
        return "Scenario{" +
                "id='" + scenarioId + '\'' +
                ", name='" + name + '\'' +
                ", changed=" + changedComponents +
                ", faults=" + injectedFaults.size() +
                '}';
    }
}
