package com.changeimpact.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

/**
 * Historical record of a single test run.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TestHistoryRecord {
    private final String testId;
    private final String buildId;
    private final Instant timestamp;
    private final boolean passed;
    private final long durationMs;
    private final String failureMessage;

    @JsonCreator
    public TestHistoryRecord(
            @JsonProperty("testId") String testId,
            @JsonProperty("buildId") String buildId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("passed") boolean passed,
            @JsonProperty("durationMs") long durationMs,
            @JsonProperty("failureMessage") String failureMessage) {
        this.testId = Objects.requireNonNull(testId, "testId cannot be null").trim();
        this.buildId = buildId != null ? buildId.trim() : "unknown";
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.passed = passed;
        this.durationMs = Math.max(1, durationMs);
        this.failureMessage = failureMessage;
    }

    public TestHistoryRecord(String testId, boolean passed, long durationMs) {
        this(testId, "manual", Instant.now(), passed, durationMs, null);
    }

    public String getTestId() {
        return testId;
    }

    public String getBuildId() {
        return buildId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public boolean isPassed() {
        return passed;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestHistoryRecord that = (TestHistoryRecord) o;
        return passed == that.passed &&
                durationMs == that.durationMs &&
                Objects.equals(testId, that.testId) &&
                Objects.equals(buildId, that.buildId) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testId, buildId, timestamp, passed, durationMs);
    }

    @Override
    public String toString() {
        return testId + " [" + (passed ? "PASS" : "FAIL") + "] " + durationMs + "ms @" + timestamp;
    }
}
