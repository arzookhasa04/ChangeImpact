package com.changeimpact.coverage;

import com.changeimpact.model.CoverageRecord;
import com.changeimpact.model.CoverageSource;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Repository mapping tests to the components/classes they exercise.
 * Distinguishes measured coverage from synthetic coverage.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CoverageRepository {

    private final Map<String, Set<String>> testToComponents;
    private final Map<String, Set<String>> componentToTests;
    private final Map<String, CoverageRecord> records;

    public CoverageRepository() {
        this.testToComponents = new HashMap<>();
        this.componentToTests = new HashMap<>();
        this.records = new LinkedHashMap<>();
    }

    @JsonCreator
    public CoverageRepository(@JsonProperty("records") List<CoverageRecord> recordList) {
        this();
        if (recordList != null) {
            for (CoverageRecord r : recordList) {
                addCoverage(r);
            }
        }
    }

    public synchronized void addCoverage(CoverageRecord record) {
        Objects.requireNonNull(record, "CoverageRecord cannot be null");
        String key = record.getTestId() + "->" + record.getComponentId();
        records.put(key, record);
        testToComponents.computeIfAbsent(record.getTestId(), k -> new LinkedHashSet<>()).add(record.getComponentId());
        componentToTests.computeIfAbsent(record.getComponentId(), k -> new LinkedHashSet<>()).add(record.getTestId());
    }

    public synchronized void addCoverage(String testId, String componentId, CoverageSource source) {
        addCoverage(new CoverageRecord(testId, componentId, source));
    }

    public Set<String> getComponentsExercisedBy(String testId) {
        return Collections.unmodifiableSet(testToComponents.getOrDefault(testId, Collections.emptySet()));
    }

    public Set<String> getTestsExercising(String componentId) {
        return Collections.unmodifiableSet(componentToTests.getOrDefault(componentId, Collections.emptySet()));
    }

    public Set<String> getAllTests() {
        return Collections.unmodifiableSet(testToComponents.keySet());
    }

    public Set<String> getAllComponents() {
        return Collections.unmodifiableSet(componentToTests.keySet());
    }

    @JsonProperty("records")
    public List<CoverageRecord> getRecords() {
        return new ArrayList<>(records.values());
    }

    public boolean hasCoverage(String testId, String componentId) {
        return records.containsKey(testId + "->" + componentId);
    }

    public CoverageRecord getRecord(String testId, String componentId) {
        return records.get(testId + "->" + componentId);
    }

    public void saveToFile(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(file, this);
    }

    public static CoverageRepository loadFromFile(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(file, CoverageRepository.class);
    }
}
