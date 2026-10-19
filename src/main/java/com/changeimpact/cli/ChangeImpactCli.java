package com.changeimpact.cli;

import com.changeimpact.analyzer.ChangeImpactAnalyzer;
import com.changeimpact.benchmark.BenchmarkDatasetGenerator;
import com.changeimpact.benchmark.EvaluationEngine;
import com.changeimpact.coverage.CoverageRepository;
import com.changeimpact.detector.FileChange;
import com.changeimpact.detector.SourceChangeDetector;
import com.changeimpact.executor.RealTestExecutor;
import com.changeimpact.graph.DependencyGraph;
import com.changeimpact.history.HistoryStore;
import com.changeimpact.model.*;
import com.changeimpact.optimizer.TestSelectionOptimizer;
import com.changeimpact.optimizer.TestSelectionStrategy;
import com.changeimpact.scorer.RiskScorer;
import com.changeimpact.scorer.RiskWeights;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Main Command-Line Interface for ChangeImpact.
 */
@Command(
        name = "changeimpact",
        mixinStandardHelpOptions = true,
        version = "ChangeImpact 1.0.0",
        description = "Risk-Aware Incremental Regression Testing Engine",
        subcommands = {
                ChangeImpactCli.AnalyzeCmd.class,
                ChangeImpactCli.SelectCmd.class,
                ChangeImpactCli.RunCmd.class,
                ChangeImpactCli.BenchmarkCmd.class,
                ChangeImpactCli.ReportCmd.class,
                ChangeImpactCli.GraphCmd.class,
                ChangeImpactCli.HistoryCmd.class,
                ChangeImpactCli.DashboardCmd.class
        }
)
public class ChangeImpactCli implements Callable<Integer> {

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new ChangeImpactCli()).execute(args);
        System.exit(exitCode);
    }

    // ==========================================
    // 1. ANALYZE COMMAND
    // ==========================================
    @Command(name = "analyze", description = "Analyze changed files and report affected components and candidate tests.")
    static class AnalyzeCmd implements Callable<Integer> {

        @Option(names = {"-c", "--changed"}, description = "Changed file(s) or component ID(s)", split = ",")
        private List<String> changedFiles;

        @Option(names = {"--git"}, description = "Detect changes automatically via Git diff")
        private boolean useGit;

        @Option(names = {"--diff-target"}, description = "Git commit or branch to diff against (e.g. HEAD~1, main)")
        private String diffTarget;

        @Option(names = {"-g", "--graph"}, description = "Path to custom dependency graph JSON file")
        private File graphFile;

        @Option(names = {"--json"}, description = "Output results in JSON format")
        private boolean jsonOutput;

        @Override
        public Integer call() throws Exception {
            BenchmarkDatasetGenerator.BenchmarkSuite suite = BenchmarkDatasetGenerator.generate();
            DependencyGraph graph = loadGraphOrDefault(graphFile, suite.getGraph());
            CoverageRepository coverage = suite.getCoverage();

            SourceChangeDetector detector = new SourceChangeDetector();
            List<FileChange> changes;

            if (useGit) {
                System.out.println("Detecting changes via Git...");
                changes = detector.detectFromGit(new File("."), diffTarget);
            } else if (changedFiles != null && !changedFiles.isEmpty()) {
                changes = detector.detectFromExplicitList(changedFiles);
            } else {
                System.err.println("Error: Must specify changed files with --changed <file> or use --git flag.");
                return 1;
            }

            Set<String> changedComponents = detector.mapChangesToComponents(changes, graph);
            if (changedComponents.isEmpty()) {
                System.out.println("No matching components found in dependency graph for specified changes.");
                return 0;
            }

            ChangeImpactAnalyzer analyzer = new ChangeImpactAnalyzer();
            ImpactReport report = analyzer.analyze(changedComponents, graph, coverage);

            if (jsonOutput) {
                ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
                System.out.println(mapper.writeValueAsString(report));
                return 0;
            }

            System.out.println("\n=== ChangeImpact Analysis Report ===");
            System.out.println("Directly Changed Components: " + report.getChangedComponentIds());
            System.out.println("Potentially Affected Components (" + report.getImpactedComponents().size() + "):");
            for (Map.Entry<String, ComponentImpact> entry : report.getImpactedComponents().entrySet()) {
                ComponentImpact imp = entry.getValue();
                System.out.printf("  - %-20s (dist: %d, direct: %s, path: %s)\n",
                        entry.getKey(), imp.getDistance(), imp.isDirectChange(), String.join(" -> ", imp.getImpactPath()));
            }

            System.out.println("\nCandidate Tests Identified (" + report.getCandidateTestIds().size() + "):");
            for (String t : report.getCandidateTestIds()) {
                System.out.println("  * " + t + " (covers: " + coverage.getComponentsExercisedBy(t) + ")");
            }

            if (report.isCycleDetected()) {
                System.out.println("\n[WARNING] Graph contains dependency cycle(s):");
                for (List<String> cycle : report.getDetectedCycles()) {
                    System.out.println("  Cycle: " + String.join(" -> ", cycle));
                }
            }

            if (!report.getWarnings().isEmpty()) {
                System.out.println("\nWarnings:");
                for (String w : report.getWarnings()) {
                    System.out.println("  ! " + w);
                }
            }

            System.out.printf("\nAnalysis completed in %d ms.\n", report.getAnalysisDurationMs());
            return 0;
        }
    }

    // ==========================================
    // 2. SELECT COMMAND
    // ==========================================
    @Command(name = "select", description = "Select and prioritize tests under time budget constraint.")
    static class SelectCmd implements Callable<Integer> {

        @Option(names = {"-c", "--changed"}, description = "Changed file(s) or component ID(s)", required = true, split = ",")
        private List<String> changedFiles;

        @Option(names = {"-s", "--strategy"}, description = "Strategy: risk-aware, full-suite, dependency-based, cost-only, failure-only", defaultValue = "risk-aware")
        private String strategyName;

        @Option(names = {"-b", "--budget"}, description = "Execution budget in seconds or milliseconds (e.g. '500ms' or '2s')", defaultValue = "500ms")
        private String budgetStr;

        @Option(names = {"--explain"}, description = "Display detailed mathematical selection explanations")
        private boolean explain;

        @Option(names = {"--json"}, description = "Output results in JSON format")
        private boolean jsonOutput;

        @Override
        public Integer call() throws Exception {
            long budgetMs = parseBudgetMs(budgetStr);
            TestSelectionStrategy strategy = parseStrategy(strategyName);

            BenchmarkDatasetGenerator.BenchmarkSuite suite = BenchmarkDatasetGenerator.generate();
            DependencyGraph graph = suite.getGraph();
            CoverageRepository coverage = suite.getCoverage();
            HistoryStore history = suite.getHistory();

            SourceChangeDetector detector = new SourceChangeDetector();
            Set<String> changedComponents = detector.mapChangesToComponents(
                    detector.detectFromExplicitList(changedFiles), graph
            );

            if (changedComponents.isEmpty()) {
                System.err.println("Error: Specified changed files/components not found in graph: " + changedFiles);
                return 1;
            }

            ChangeImpactAnalyzer analyzer = new ChangeImpactAnalyzer();
            ImpactReport impactReport = analyzer.analyze(changedComponents, graph, coverage);

            RiskScorer scorer = new RiskScorer();
            List<TestCandidate> candidates = scorer.scoreCandidates(
                    impactReport.getCandidateTestIds(),
                    impactReport.getImpactedComponents(),
                    coverage,
                    history,
                    50L
            );

            List<TestCandidate> allSuiteTests = buildAllSuiteTests(suite);

            TestSelectionOptimizer optimizer = new TestSelectionOptimizer();
            TestSelectionResult result = optimizer.selectTests(
                    strategy,
                    candidates,
                    allSuiteTests,
                    impactReport.getImpactedComponents().keySet(),
                    budgetMs,
                    scorer.getWeights(),
                    null
            );

            if (jsonOutput) {
                ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
                System.out.println(mapper.writeValueAsString(result));
                return 0;
            }

            System.out.println("\n=== ChangeImpact Test Selection ===");
            System.out.printf("Strategy: %s | Budget: %d ms | Used: %d ms (%.1f%% of full suite)\n",
                    result.getStrategyName(), result.getTotalBudgetMs(), result.getUsedBudgetMs(),
                    (result.getFullSuiteDurationMs() > 0 ? (double) result.getUsedBudgetMs() / result.getFullSuiteDurationMs() * 100 : 0.0));
            System.out.printf("Affected Components: %s\n", impactReport.getImpactedComponents().keySet());
            System.out.printf("Selected Tests (%d of %d candidates, %d total suite):\n",
                    result.getSelectedTests().size(), candidates.size(), allSuiteTests.size());

            int rank = 1;
            for (TestCandidate t : result.getSelectedTests()) {
                System.out.printf("  [%d] %-28s Cost: %4dms | RiskScore: %.4f | CoversAffected: %s\n",
                        rank++, t.getTestId(), t.getCostMs(), t.getCompositeRiskScore(), t.getAffectedComponents());
            }

            System.out.printf("\nAffected Components Coverage: %.1f%% (%d of %d)\n",
                    result.getAffectedCoverageRatio() * 100,
                    result.getCoveredAffectedComponents().size(),
                    result.getTotalAffectedComponents().size());

            if (result.getSelectedTests().isEmpty()) {
                System.out.println("[NOTICE] No tests could be selected within the provided budget of " + budgetMs + "ms.");
            }

            if (explain) {
                System.out.println("\n=== Detailed Selection Explanations ===");
                for (SelectionExplanation exp : result.getExplanations()) {
                    System.out.println(exp);
                }
            }

            return 0;
        }
    }

    // ==========================================
    // 3. RUN COMMAND
    // ==========================================
    @Command(name = "run", description = "Select and execute regression tests via JUnit or simulation.")
    static class RunCmd implements Callable<Integer> {

        @Option(names = {"-c", "--changed"}, description = "Changed file(s) or component ID(s)", required = true, split = ",")
        private List<String> changedFiles;

        @Option(names = {"-s", "--strategy"}, description = "Strategy: risk-aware, full-suite, dependency-based, cost-only, failure-only", defaultValue = "risk-aware")
        private String strategyName;

        @Option(names = {"-b", "--budget"}, description = "Execution budget (e.g. 500ms or 2s)", defaultValue = "500ms")
        private String budgetStr;

        @Option(names = {"--simulate"}, description = "Simulate test execution instead of launching JUnit")
        private boolean simulate;

        @Override
        public Integer call() throws Exception {
            long budgetMs = parseBudgetMs(budgetStr);
            TestSelectionStrategy strategy = parseStrategy(strategyName);

            BenchmarkDatasetGenerator.BenchmarkSuite suite = BenchmarkDatasetGenerator.generate();
            SourceChangeDetector detector = new SourceChangeDetector();
            Set<String> changedComponents = detector.mapChangesToComponents(
                    detector.detectFromExplicitList(changedFiles), suite.getGraph()
            );

            ChangeImpactAnalyzer analyzer = new ChangeImpactAnalyzer();
            ImpactReport impactReport = analyzer.analyze(changedComponents, suite.getGraph(), suite.getCoverage());

            RiskScorer scorer = new RiskScorer();
            List<TestCandidate> candidates = scorer.scoreCandidates(
                    impactReport.getCandidateTestIds(),
                    impactReport.getImpactedComponents(),
                    suite.getCoverage(),
                    suite.getHistory(),
                    50L
            );

            List<TestCandidate> allSuiteTests = buildAllSuiteTests(suite);
            TestSelectionOptimizer optimizer = new TestSelectionOptimizer();
            TestSelectionResult selection = optimizer.selectTests(
                    strategy,
                    candidates,
                    allSuiteTests,
                    impactReport.getImpactedComponents().keySet(),
                    budgetMs,
                    scorer.getWeights(),
                    null
            );

            System.out.printf("\nExecuting %d selected tests under '%s'...\n",
                    selection.getSelectedTests().size(), strategy);

            if (simulate) {
                System.out.println("[Mode: SIMULATED EXECUTION]");
                long totalTime = 0;
                for (TestCandidate t : selection.getSelectedTests()) {
                    totalTime += t.getCostMs();
                    System.out.printf("  * %-25s -> SUCCESS (simulated %d ms)\n", t.getTestId(), t.getCostMs());
                }
                System.out.printf("\nSimulated execution completed: %d tests in %d ms.\n",
                        selection.getSelectedTests().size(), totalTime);
            } else {
                System.out.println("[Mode: REAL JUNIT PLATFORM LAUNCHER]");
                RealTestExecutor executor = new RealTestExecutor();
                RealTestExecutor.RealExecutionReport report = executor.executeTests(selection.getSelectedTests(), suite.getHistory());

                for (RealTestExecutor.TestExecutionOutcome outcome : report.getOutcomes()) {
                    System.out.printf("  * %-25s -> %s (actual %d ms)%s\n",
                            outcome.getTestId(),
                            outcome.isPassed() ? "PASS" : "FAIL",
                            outcome.getActualDurationMs(),
                            outcome.getFailureMessage() != null ? " [" + outcome.getFailureMessage() + "]" : "");
                }

                System.out.printf("\nExecution completed: %d passed, %d failed in actual %d ms.\n",
                        report.getPassedCount(), report.getFailedCount(), report.getTotalExecutionDurationMs());
            }

            return 0;
        }
    }

    // ==========================================
    // 4. BENCHMARK COMMAND
    // ==========================================
    @Command(name = "benchmark", description = "Run reproducible benchmark suite comparing all strategies across budgets.")
    static class BenchmarkCmd implements Callable<Integer> {

        @Option(names = {"--output-csv"}, description = "Path to export benchmark results CSV", defaultValue = "benchmarks/results/benchmark_results.csv")
        private File outputCsv;

        @Option(names = {"--output-report"}, description = "Path to export markdown evaluation report", defaultValue = "benchmarks/results/benchmark_report.md")
        private File outputReport;

        @Option(names = {"--visualize"}, description = "Generate charts using Python matplotlib")
        private boolean visualize;

        @Override
        public Integer call() throws Exception {
            System.out.println("\n=======================================================");
            System.out.println("  ChangeImpact: Systematic Benchmark Evaluation Suite  ");
            System.out.println("=======================================================\n");

            BenchmarkDatasetGenerator.BenchmarkSuite suite = BenchmarkDatasetGenerator.generate();
            System.out.printf("Loaded Benchmark Suite: %d components, %d edges, %d tests, %d scenarios\n",
                    suite.getGraph().getNodeCount(),
                    suite.getGraph().getEdgeCount(),
                    suite.getTestEstimatedCosts().size(),
                    suite.getScenarios().size());

            EvaluationEngine engine = new EvaluationEngine();
            System.out.println("Executing multi-strategy matrix across all scenarios and budget fractions...");

            List<StrategyComparison> comparisons = engine.runBenchmark(suite);

            engine.exportToCsv(comparisons, outputCsv);
            System.out.printf("-> CSV results exported to: %s (%d rows)\n", outputCsv.getAbsolutePath(), comparisons.size());

            String markdown = engine.generateMarkdownReport(comparisons);
            if (outputReport.getParentFile() != null) {
                outputReport.getParentFile().mkdirs();
            }
            Files.writeString(outputReport.toPath(), markdown);
            System.out.printf("-> Markdown report exported to: %s\n", outputReport.getAbsolutePath());

            if (visualize) {
                System.out.println("\nRunning Python visualizer to generate benchmark charts...");
                runPythonVisualizer(outputCsv);
            }

            System.out.println("\nBenchmark complete!");
            return 0;
        }

        private void runPythonVisualizer(File csvFile) {
            try {
                // Find python
                String pythonCmd = "python";
                File localPy = new File(System.getProperty("user.home") + "/AppData/Local/Programs/Python/Python312/python.exe");
                if (localPy.exists()) {
                    pythonCmd = localPy.getAbsolutePath();
                }

                File scriptFile = new File("scripts/visualize_benchmarks.py");
                if (!scriptFile.exists()) {
                    System.err.println("Warning: Visualizer script not found at " + scriptFile.getAbsolutePath());
                    return;
                }

                ProcessBuilder pb = new ProcessBuilder(
                        pythonCmd,
                        scriptFile.getAbsolutePath(),
                        "--csv", csvFile.getAbsolutePath(),
                        "--output-dir", "benchmarks/results/charts"
                );
                pb.inheritIO();
                Process p = pb.start();
                int exitCode = p.waitFor();
                if (exitCode == 0) {
                    System.out.println("-> Charts generated successfully in benchmarks/results/charts/");
                } else {
                    System.err.println("Warning: Visualizer script returned code " + exitCode);
                }
            } catch (Exception e) {
                System.err.println("Warning: Chart generation failed: " + e.getMessage());
            }
        }
    }

    // ==========================================
    // 5. REPORT COMMAND
    // ==========================================
    @Command(name = "report", description = "Display summary report of latest benchmark or evaluation.")
    static class ReportCmd implements Callable<Integer> {

        @Option(names = {"-f", "--file"}, description = "Path to report file", defaultValue = "benchmarks/results/benchmark_report.md")
        private File reportFile;

        @Override
        public Integer call() throws Exception {
            if (!reportFile.exists()) {
                System.err.println("Report file not found: " + reportFile.getAbsolutePath());
                System.err.println("Run 'changeimpact benchmark' first to generate results.");
                return 1;
            }
            String content = Files.readString(reportFile.toPath());
            System.out.println(content);
            return 0;
        }
    }

    // ==========================================
    // 6. GRAPH COMMAND
    // ==========================================
    @Command(name = "graph", description = "Inspect dependency graph structure, statistics, and cycles.")
    static class GraphCmd implements Callable<Integer> {

        @Option(names = {"-g", "--graph"}, description = "Path to graph JSON file")
        private File graphFile;

        @Option(names = {"--cycles"}, description = "Detect and display dependency cycles")
        private boolean detectCycles;

        @Override
        public Integer call() throws Exception {
            BenchmarkDatasetGenerator.BenchmarkSuite suite = BenchmarkDatasetGenerator.generate();
            DependencyGraph graph = loadGraphOrDefault(graphFile, suite.getGraph());

            System.out.println("\n=== Dependency Graph Inspection ===");
            System.out.printf("Nodes: %d | Edges: %d\n", graph.getNodeCount(), graph.getEdgeCount());

            System.out.println("\nGraph Nodes:");
            for (Node n : graph.getNodes()) {
                System.out.printf("  * %-22s [%s] %s\n",
                        n.getId(), n.getType(), n.getFilePath() != null ? "(" + n.getFilePath() + ")" : "");
            }

            List<List<String>> cycles = graph.detectCycles();
            System.out.printf("\nDependency Cycles Detected: %d\n", cycles.size());
            for (int i = 0; i < cycles.size(); i++) {
                System.out.printf("  Cycle #%d: %s\n", (i + 1), String.join(" -> ", cycles.get(i)));
            }

            return 0;
        }
    }

    // ==========================================
    // 7. HISTORY COMMAND
    // ==========================================
    @Command(name = "history", description = "Inspect and query test execution history store.")
    static class HistoryCmd implements Callable<Integer> {

        @Option(names = {"-t", "--test"}, description = "Query specific test ID")
        private String testId;

        @Override
        public Integer call() throws Exception {
            BenchmarkDatasetGenerator.BenchmarkSuite suite = BenchmarkDatasetGenerator.generate();
            HistoryStore history = suite.getHistory();

            System.out.println("\n=== Test Execution History Store ===");
            System.out.printf("Total historical records: %d\n", history.getAllRecords().size());

            if (testId != null) {
                if (!history.hasHistory(testId)) {
                    System.out.println("No history found for test: " + testId);
                    return 0;
                }
                List<TestHistoryRecord> records = history.getHistoryForTest(testId);
                System.out.printf("\nHistory for '%s' (%d runs):\n", testId, records.size());
                System.out.printf("  - Raw Failure Rate: %.2f%%\n", history.getRawFailureRate(testId) * 100);
                System.out.printf("  - Laplace Smoothed: %.2f%%\n", history.getSmoothedFailureRate(testId) * 100);
                System.out.printf("  - Recency-Weighted: %.2f%%\n", history.getRecencyWeightedFailureSignal(testId, 0.85) * 100);
                System.out.printf("  - Mean Duration:    %d ms\n", history.getMeanDurationMs(testId, 50));
            } else {
                System.out.println("\nRecent Test Records (sample):");
                List<TestHistoryRecord> records = history.getAllRecords();
                int limit = Math.min(10, records.size());
                for (int i = 0; i < limit; i++) {
                    System.out.println("  " + records.get(i));
                }
            }

            return 0;
        }
    }

    // ==========================================
    // 8. DASHBOARD COMMAND
    // ==========================================
    @Command(name = "dashboard", description = "Launch or view interactive web dashboard.")
    static class DashboardCmd implements Callable<Integer> {

        @Option(names = {"-p", "--port"}, description = "Port number for lightweight dashboard server", defaultValue = "8080")
        private int port;

        @Option(names = {"--file-only"}, description = "Print absolute path to dashboard HTML without launching web server")
        private boolean fileOnly;

        @Override
        public Integer call() throws Exception {
            File dashFile = new File("dashboard/index.html");
            if (!dashFile.exists()) {
                System.err.println("Dashboard HTML not found at " + dashFile.getAbsolutePath());
                return 1;
            }

            System.out.println("\n=== ChangeImpact Web Dashboard ===");
            System.out.println("Dashboard File: " + dashFile.getAbsolutePath());
            System.out.println("You can open this file directly in any modern web browser:");
            System.out.println("  file:///" + dashFile.getAbsolutePath().replace('\\', '/'));

            if (fileOnly) {
                return 0;
            }

            try {
                com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(port), 0);
                server.createContext("/", exchange -> {
                    String path = exchange.getRequestURI().getPath();
                    File requestedFile;
                    if (path == null || path.equals("/") || path.isEmpty()) {
                        requestedFile = new File("dashboard/index.html");
                    } else if (path.startsWith("/benchmarks/results/charts/")) {
                        requestedFile = new File(path.substring(1));
                    } else {
                        requestedFile = new File("dashboard" + path);
                    }

                    if (requestedFile.exists() && requestedFile.isFile()) {
                        byte[] bytes = java.nio.file.Files.readAllBytes(requestedFile.toPath());
                        String contentType = "text/html";
                        if (requestedFile.getName().endsWith(".png")) contentType = "image/png";
                        else if (requestedFile.getName().endsWith(".css")) contentType = "text/css";
                        else if (requestedFile.getName().endsWith(".js")) contentType = "application/javascript";

                        exchange.getResponseHeaders().set("Content-Type", contentType);
                        exchange.sendResponseHeaders(200, bytes.length);
                        try (java.io.OutputStream os = exchange.getResponseBody()) {
                            os.write(bytes);
                        }
                    } else {
                        byte[] notFound = "404 Not Found".getBytes();
                        exchange.sendResponseHeaders(404, notFound.length);
                        try (java.io.OutputStream os = exchange.getResponseBody()) {
                            os.write(notFound);
                        }
                    }
                });

                server.start();
                System.out.printf("\nDashboard server listening at: http://localhost:%d/\n", port);
                System.out.println("Press Ctrl+C to stop server (or run with --file-only for static viewing).");

                // Keep alive for 500ms before returning in non-blocking environments, or keep running
                Thread.sleep(1500);
            } catch (Exception e) {
                System.out.println("Note: Web server initialization skipped (" + e.getMessage() + "). Please open HTML file directly.");
            }

            return 0;
        }
    }

    // ==========================================
    // Helper Methods
    // ==========================================
    private static DependencyGraph loadGraphOrDefault(File file, DependencyGraph fallback) {
        if (file != null && file.exists()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(file, DependencyGraph.class);
            } catch (IOException e) {
                System.err.println("Warning: Failed to load graph from " + file + ", using default: " + e.getMessage());
            }
        }
        return fallback;
    }

    private static long parseBudgetMs(String budgetStr) {
        if (budgetStr == null || budgetStr.trim().isEmpty()) {
            return 500L;
        }
        String clean = budgetStr.trim().toLowerCase();
        if (clean.endsWith("ms")) {
            return Long.parseLong(clean.replace("ms", "").trim());
        } else if (clean.endsWith("s")) {
            return (long) (Double.parseDouble(clean.replace("s", "").trim()) * 1000);
        } else {
            return Long.parseLong(clean);
        }
    }

    private static TestSelectionStrategy parseStrategy(String strategy) {
        if (strategy == null) return TestSelectionStrategy.RISK_AWARE_BUDGETED;
        String s = strategy.trim().toLowerCase().replace("-", "_");
        switch (s) {
            case "full":
            case "full_suite":
                return TestSelectionStrategy.FULL_SUITE;
            case "dep":
            case "dependency":
            case "dependency_based":
                return TestSelectionStrategy.DEPENDENCY_BASED;
            case "cost":
            case "cost_only":
            case "greedy_cost_only":
                return TestSelectionStrategy.GREEDY_COST_ONLY;
            case "fail":
            case "failure":
            case "failure_only":
            case "greedy_failure_only":
                return TestSelectionStrategy.GREEDY_FAILURE_ONLY;
            case "risk":
            case "risk_aware":
            case "risk_aware_budgeted":
            default:
                return TestSelectionStrategy.RISK_AWARE_BUDGETED;
        }
    }

    private static List<TestCandidate> buildAllSuiteTests(BenchmarkDatasetGenerator.BenchmarkSuite suite) {
        List<TestCandidate> list = new ArrayList<>();
        for (String testId : suite.getTestEstimatedCosts().keySet()) {
            long cost = suite.getTestEstimatedCosts().get(testId);
            Set<String> covered = suite.getCoverage().getComponentsExercisedBy(testId);
            double failureSignal = suite.getHistory().getSmoothedFailureRate(testId);
            list.add(new TestCandidate(
                    testId,
                    testId,
                    null,
                    cost,
                    covered,
                    Collections.emptySet(),
                    0.0,
                    failureSignal,
                    0.0,
                    1.0 / Math.sqrt(suite.getHistory().getTotalRuns(testId) + 1.0),
                    0.5
            ));
        }
        return list;
    }
}
