package com.changeimpact.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Benchmark metrics comparing test selection strategies for a scenario.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class StrategyComparison {
    private final String scenarioId;
    private final String scenarioName;
    private final String strategyName;
    private final long budgetMs;
    private final int selectedTestCount;
    private final int candidateTestCount;
    private final int fullSuiteTestCount;
    private final double selectionRatio;
    private final long usedBudgetMs;
    private final long fullSuiteDurationMs;
    private final double timeReductionPct;
    private final int affectedCovered;
    private final int affectedTotal;
    private final double affectedCoveragePct;
    private final int detectedFaultCount;
    private final int totalFaultCount;
    private final double faultDetectionRatePct;
    private final double precision;
    private final double recall;
    private final long analysisLatencyMs;
    private final long memoryBytes;

    @JsonCreator
    public StrategyComparison(
            @JsonProperty("scenarioId") String scenarioId,
            @JsonProperty("scenarioName") String scenarioName,
            @JsonProperty("strategyName") String strategyName,
            @JsonProperty("budgetMs") long budgetMs,
            @JsonProperty("selectedTestCount") int selectedTestCount,
            @JsonProperty("candidateTestCount") int candidateTestCount,
            @JsonProperty("fullSuiteTestCount") int fullSuiteTestCount,
            @JsonProperty("selectionRatio") double selectionRatio,
            @JsonProperty("usedBudgetMs") long usedBudgetMs,
            @JsonProperty("fullSuiteDurationMs") long fullSuiteDurationMs,
            @JsonProperty("timeReductionPct") double timeReductionPct,
            @JsonProperty("affectedCovered") int affectedCovered,
            @JsonProperty("affectedTotal") int affectedTotal,
            @JsonProperty("affectedCoveragePct") double affectedCoveragePct,
            @JsonProperty("detectedFaultCount") int detectedFaultCount,
            @JsonProperty("totalFaultCount") int totalFaultCount,
            @JsonProperty("faultDetectionRatePct") double faultDetectionRatePct,
            @JsonProperty("precision") double precision,
            @JsonProperty("recall") double recall,
            @JsonProperty("analysisLatencyMs") long analysisLatencyMs,
            @JsonProperty("memoryBytes") long memoryBytes) {
        this.scenarioId = Objects.requireNonNull(scenarioId, "scenarioId cannot be null");
        this.scenarioName = scenarioName != null ? scenarioName : scenarioId;
        this.strategyName = Objects.requireNonNull(strategyName, "strategyName cannot be null");
        this.budgetMs = budgetMs;
        this.selectedTestCount = selectedTestCount;
        this.candidateTestCount = candidateTestCount;
        this.fullSuiteTestCount = fullSuiteTestCount;
        this.selectionRatio = selectionRatio;
        this.usedBudgetMs = usedBudgetMs;
        this.fullSuiteDurationMs = fullSuiteDurationMs;
        this.timeReductionPct = timeReductionPct;
        this.affectedCovered = affectedCovered;
        this.affectedTotal = affectedTotal;
        this.affectedCoveragePct = affectedCoveragePct;
        this.detectedFaultCount = detectedFaultCount;
        this.totalFaultCount = totalFaultCount;
        this.faultDetectionRatePct = faultDetectionRatePct;
        this.precision = precision;
        this.recall = recall;
        this.analysisLatencyMs = analysisLatencyMs;
        this.memoryBytes = memoryBytes;
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public long getBudgetMs() {
        return budgetMs;
    }

    public int getSelectedTestCount() {
        return selectedTestCount;
    }

    public int getCandidateTestCount() {
        return candidateTestCount;
    }

    public int getFullSuiteTestCount() {
        return fullSuiteTestCount;
    }

    public double getSelectionRatio() {
        return selectionRatio;
    }

    public long getUsedBudgetMs() {
        return usedBudgetMs;
    }

    public long getFullSuiteDurationMs() {
        return fullSuiteDurationMs;
    }

    public double getTimeReductionPct() {
        return timeReductionPct;
    }

    public int getAffectedCovered() {
        return affectedCovered;
    }

    public int getAffectedTotal() {
        return affectedTotal;
    }

    public double getAffectedCoveragePct() {
        return affectedCoveragePct;
    }

    public int getDetectedFaultCount() {
        return detectedFaultCount;
    }

    public int getTotalFaultCount() {
        return totalFaultCount;
    }

    public double getFaultDetectionRatePct() {
        return faultDetectionRatePct;
    }

    public double getPrecision() {
        return precision;
    }

    public double getRecall() {
        return recall;
    }

    public long getAnalysisLatencyMs() {
        return analysisLatencyMs;
    }

    public long getMemoryBytes() {
        return memoryBytes;
    }

    public String toCsvRow() {
        return String.format("%s,%s,%s,%d,%d,%d,%d,%.4f,%d,%d,%.2f,%d,%d,%.2f,%d,%d,%.2f,%.4f,%.4f,%d,%d",
                scenarioId,
                escapeCsv(scenarioName),
                escapeCsv(strategyName),
                budgetMs,
                selectedTestCount,
                candidateTestCount,
                fullSuiteTestCount,
                selectionRatio,
                usedBudgetMs,
                fullSuiteDurationMs,
                timeReductionPct,
                affectedCovered,
                affectedTotal,
                affectedCoveragePct,
                detectedFaultCount,
                totalFaultCount,
                faultDetectionRatePct,
                precision,
                recall,
                analysisLatencyMs,
                memoryBytes);
    }

    public static String getCsvHeader() {
        return "scenario_id,scenario_name,strategy_name,budget_ms,selected_test_count,candidate_test_count,full_suite_test_count,selection_ratio,used_budget_ms,full_suite_duration_ms,time_reduction_pct,affected_covered,affected_total,affected_coverage_pct,detected_fault_count,total_fault_count,fault_detection_rate_pct,precision,recall,analysis_latency_ms,memory_bytes";
    }

    private static String escapeCsv(String str) {
        if (str.contains(",") || str.contains("\"")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }
}
