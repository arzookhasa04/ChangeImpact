package com.changeimpact.benchmark;

import com.changeimpact.model.StrategyComparison;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EvaluationEngineTest {

    @Test
    @DisplayName("Should run complete benchmark matrix and export CSV and Markdown correctly")
    public void testBenchmarkExecution(@TempDir File tempDir) throws IOException {
        BenchmarkDatasetGenerator.BenchmarkSuite suite = BenchmarkDatasetGenerator.generate();
        EvaluationEngine engine = new EvaluationEngine();

        List<StrategyComparison> comparisons = engine.runBenchmark(suite);

        assertFalse(comparisons.isEmpty(), "Comparisons should not be empty");
        // 5 scenarios * (1 Full + 1 Dep + 4 budgets * 3 strategies) = 5 * 14 = 70 comparison rows
        assertEquals(70, comparisons.size());

        // Verify CSV export
        File csvFile = new File(tempDir, "benchmark_results.csv");
        engine.exportToCsv(comparisons, csvFile);
        assertTrue(csvFile.exists());
        List<String> lines = Files.readAllLines(csvFile.toPath());
        assertEquals(71, lines.size(), "CSV should have header + 70 rows");
        assertTrue(lines.getFirst().startsWith("scenario_id,scenario_name"));

        // Verify Markdown generation
        String md = engine.generateMarkdownReport(comparisons);
        assertNotNull(md);
        assertTrue(md.contains("# ChangeImpact Engine: Benchmark Evaluation Report"));
        assertTrue(md.contains("RISK_AWARE_BUDGETED"));
        assertTrue(md.contains("FULL_SUITE"));
    }
}
