package com.changeimpact.history;

import com.changeimpact.model.TestHistoryRecord;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Persistent store for test execution history, providing failure signals and duration statistics.
 * Gracefully handles missing history without crashing or fabricating fake certainty.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HistoryStore {

    private final Map<String, List<TestHistoryRecord>> historyByTest;

    public HistoryStore() {
        this.historyByTest = new LinkedHashMap<>();
    }

    @JsonCreator
    public HistoryStore(@JsonProperty("records") List<TestHistoryRecord> records) {
        this();
        if (records != null) {
            for (TestHistoryRecord r : records) {
                addRecord(r);
            }
        }
    }

    public synchronized void addRecord(TestHistoryRecord record) {
        Objects.requireNonNull(record, "TestHistoryRecord cannot be null");
        historyByTest.computeIfAbsent(record.getTestId(), k -> new ArrayList<>()).add(record);
    }

    public List<TestHistoryRecord> getHistoryForTest(String testId) {
        List<TestHistoryRecord> list = historyByTest.get(testId);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    @JsonProperty("records")
    public List<TestHistoryRecord> getAllRecords() {
        List<TestHistoryRecord> all = new ArrayList<>();
        for (List<TestHistoryRecord> list : historyByTest.values()) {
            all.addAll(list);
        }
        return all;
    }

    public int getTotalRuns(String testId) {
        List<TestHistoryRecord> list = historyByTest.get(testId);
        return list != null ? list.size() : 0;
    }

    public boolean hasHistory(String testId) {
        return historyByTest.containsKey(testId) && !historyByTest.get(testId).isEmpty();
    }

    /**
     * Compute empirical failure rate with Laplace smoothing: (failures + 1) / (totalRuns + 2).
     * If no runs exist, returns 0.5 (uninformative prior), acknowledging maximum uncertainty.
     */
    public double getSmoothedFailureRate(String testId) {
        List<TestHistoryRecord> list = historyByTest.get(testId);
        if (list == null || list.isEmpty()) {
            return 0.5; // Uninformative prior for unseen test
        }
        long failures = list.stream().filter(r -> !r.isPassed()).count();
        return (double) (failures + 1) / (list.size() + 2);
    }

    /**
     * Compute raw failure rate: failures / totalRuns. Returns 0.0 if no runs exist.
     */
    public double getRawFailureRate(String testId) {
        List<TestHistoryRecord> list = historyByTest.get(testId);
        if (list == null || list.isEmpty()) {
            return 0.0;
        }
        long failures = list.stream().filter(r -> !r.isPassed()).count();
        return (double) failures / list.size();
    }

    /**
     * Compute recency-weighted failure signal using exponential decay.
     * Weights recent runs higher than older runs.
     *
     * @param testId      test identifier
     * @param decayFactor factor in (0, 1], typically 0.85
     * @return weighted failure score in [0.0, 1.0]
     */
    public double getRecencyWeightedFailureSignal(String testId, double decayFactor) {
        List<TestHistoryRecord> list = historyByTest.get(testId);
        if (list == null || list.isEmpty()) {
            return 0.5; // Default uninformative prior
        }

        // Sort descending by timestamp (most recent first)
        List<TestHistoryRecord> sorted = new ArrayList<>(list);
        sorted.sort(Comparator.comparing(TestHistoryRecord::getTimestamp).reversed());

        double totalWeight = 0.0;
        double weightedFailures = 0.0;
        double currentWeight = 1.0;

        for (TestHistoryRecord r : sorted) {
            totalWeight += currentWeight;
            if (!r.isPassed()) {
                weightedFailures += currentWeight;
            }
            currentWeight *= decayFactor;
        }

        return totalWeight > 0 ? (weightedFailures / totalWeight) : 0.5;
    }

    /**
     * Compute mean execution duration for a test. Returns fallbackDefaultMs if no history is available.
     */
    public long getMeanDurationMs(String testId, long fallbackDefaultMs) {
        List<TestHistoryRecord> list = historyByTest.get(testId);
        if (list == null || list.isEmpty()) {
            return Math.max(1, fallbackDefaultMs);
        }
        long sum = 0;
        for (TestHistoryRecord r : list) {
            sum += r.getDurationMs();
        }
        return Math.max(1, sum / list.size());
    }

    public void saveToFile(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(file, this);
    }

    public static HistoryStore loadFromFile(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        return mapper.readValue(file, HistoryStore.class);
    }
}
