package com.changeimpact.executor;

import com.changeimpact.history.HistoryStore;
import com.changeimpact.model.TestCandidate;
import com.changeimpact.model.TestHistoryRecord;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.time.Instant;
import java.util.*;

/**
 * Genuinely executes selected JUnit 5 tests via the JUnit Platform Launcher,
 * recording actual execution durations and outcomes.
 */
public class RealTestExecutor {

    public RealTestExecutor() {
    }

    /**
     * Executes the given test candidates using JUnit Platform Launcher.
     *
     * @param selectedTests list of test candidates to execute
     * @param historyStore  history store to update with actual run outcomes
     * @return ExecutionResult containing real durations, pass/fail status, and summary
     */
    public RealExecutionReport executeTests(List<TestCandidate> selectedTests, HistoryStore historyStore) {
        long overallStart = System.currentTimeMillis();
        List<TestExecutionOutcome> outcomes = new ArrayList<>();

        if (selectedTests == null || selectedTests.isEmpty()) {
            return new RealExecutionReport(Collections.emptyList(), 0, 0, 0, 0);
        }

        Launcher launcher = LauncherFactory.create();
        int passedCount = 0;
        int failedCount = 0;

        for (TestCandidate candidate : selectedTests) {
            String testClassName = candidate.getTestName();
            long testStart = System.currentTimeMillis();
            boolean passed = false;
            String failureMessage = null;

            try {
                Class<?> testClass = Class.forName(testClassName);
                LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                        .selectors(DiscoverySelectors.selectClass(testClass))
                        .build();

                SummaryGeneratingListener listener = new SummaryGeneratingListener();
                launcher.execute(request, listener);

                TestExecutionSummary summary = listener.getSummary();
                long duration = Math.max(1, System.currentTimeMillis() - testStart);

                if (summary.getTotalFailureCount() == 0 && summary.getTestsSucceededCount() > 0) {
                    passed = true;
                    passedCount++;
                } else {
                    passed = false;
                    failedCount++;
                    if (!summary.getFailures().isEmpty()) {
                        failureMessage = summary.getFailures().getFirst().getException().getMessage();
                    } else {
                        failureMessage = "No tests succeeded or test failed assertion.";
                    }
                }

                TestExecutionOutcome outcome = new TestExecutionOutcome(
                        candidate.getTestId(),
                        testClassName,
                        passed,
                        duration,
                        failureMessage
                );
                outcomes.add(outcome);

                // Update history store with genuine execution
                if (historyStore != null) {
                    historyStore.addRecord(new TestHistoryRecord(
                            candidate.getTestId(),
                            "real-exec-" + System.currentTimeMillis(),
                            Instant.now(),
                            passed,
                            duration,
                            failureMessage
                    ));
                }

            } catch (ClassNotFoundException e) {
                // If class cannot be dynamically loaded by name, record error
                long duration = Math.max(1, System.currentTimeMillis() - testStart);
                failedCount++;
                failureMessage = "Class not found on classpath: " + testClassName;
                outcomes.add(new TestExecutionOutcome(
                        candidate.getTestId(),
                        testClassName,
                        false,
                        duration,
                        failureMessage
                ));
            } catch (Exception e) {
                long duration = Math.max(1, System.currentTimeMillis() - testStart);
                failedCount++;
                failureMessage = "Execution error: " + e.getMessage();
                outcomes.add(new TestExecutionOutcome(
                        candidate.getTestId(),
                        testClassName,
                        false,
                        duration,
                        failureMessage
                ));
            }
        }

        long totalDuration = System.currentTimeMillis() - overallStart;
        return new RealExecutionReport(outcomes, totalDuration, passedCount, failedCount, selectedTests.size());
    }

    public static class TestExecutionOutcome {
        private final String testId;
        private final String className;
        private final boolean passed;
        private final long actualDurationMs;
        private final String failureMessage;

        public TestExecutionOutcome(String testId, String className, boolean passed, long actualDurationMs, String failureMessage) {
            this.testId = testId;
            this.className = className;
            this.passed = passed;
            this.actualDurationMs = actualDurationMs;
            this.failureMessage = failureMessage;
        }

        public String getTestId() {
            return testId;
        }

        public String getClassName() {
            return className;
        }

        public boolean isPassed() {
            return passed;
        }

        public long getActualDurationMs() {
            return actualDurationMs;
        }

        public String getFailureMessage() {
            return failureMessage;
        }

        @Override
        public String toString() {
            return testId + " (" + (passed ? "PASS" : "FAIL") + " in " + actualDurationMs + "ms)"
                    + (failureMessage != null ? " - " + failureMessage : "");
        }
    }

    public static class RealExecutionReport {
        private final List<TestExecutionOutcome> outcomes;
        private final long totalExecutionDurationMs;
        private final int passedCount;
        private final int failedCount;
        private final int totalExecuted;

        public RealExecutionReport(List<TestExecutionOutcome> outcomes, long totalExecutionDurationMs, int passedCount, int failedCount, int totalExecuted) {
            this.outcomes = outcomes;
            this.totalExecutionDurationMs = totalExecutionDurationMs;
            this.passedCount = passedCount;
            this.failedCount = failedCount;
            this.totalExecuted = totalExecuted;
        }

        public List<TestExecutionOutcome> getOutcomes() {
            return outcomes;
        }

        public long getTotalExecutionDurationMs() {
            return totalExecutionDurationMs;
        }

        public int getPassedCount() {
            return passedCount;
        }

        public int getFailedCount() {
            return failedCount;
        }

        public int getTotalExecuted() {
            return totalExecuted;
        }
    }
}
