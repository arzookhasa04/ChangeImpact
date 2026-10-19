package com.changeimpact.benchmark;

import com.changeimpact.coverage.CoverageRepository;
import com.changeimpact.graph.DependencyGraph;
import com.changeimpact.history.HistoryStore;
import com.changeimpact.model.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Generates a deterministic benchmark software project graph with:
 * - 12 interconnected modules/components
 * - Direct and transitive dependencies
 * - Explicit dependency cycles (OrderService <-> PaymentGateway <-> FraudDetector)
 * - Tests with overlapping coverage and varying execution costs (10ms to 1800ms)
 * - Historical execution records with realistic failure distributions
 * - Injected fault ground truth for multiple scenarios
 * Uses a fixed random seed (42) for total reproducibility.
 */
public class BenchmarkDatasetGenerator {

    public static final long SEED = 42L;

    public static class BenchmarkSuite {
        private final DependencyGraph graph;
        private final CoverageRepository coverage;
        private final HistoryStore history;
        private final List<Scenario> scenarios;
        private final Map<String, Long> testEstimatedCosts;

        public BenchmarkSuite(
                DependencyGraph graph,
                CoverageRepository coverage,
                HistoryStore history,
                List<Scenario> scenarios,
                Map<String, Long> testEstimatedCosts) {
            this.graph = graph;
            this.coverage = coverage;
            this.history = history;
            this.scenarios = scenarios;
            this.testEstimatedCosts = testEstimatedCosts;
        }

        public DependencyGraph getGraph() {
            return graph;
        }

        public CoverageRepository getCoverage() {
            return coverage;
        }

        public HistoryStore getHistory() {
            return history;
        }

        public List<Scenario> getScenarios() {
            return scenarios;
        }

        public Map<String, Long> getTestEstimatedCosts() {
            return testEstimatedCosts;
        }
    }

    public static BenchmarkSuite generate() {
        DependencyGraph graph = new DependencyGraph();
        CoverageRepository coverage = new CoverageRepository();
        HistoryStore history = new HistoryStore();
        Map<String, Long> costs = new LinkedHashMap<>();

        // 1. Add Components
        String[] components = {
                "AuthService", "UserService", "AccountService", "OrderService",
                "InventoryService", "PaymentGateway", "FraudDetector", "AuditLogger",
                "NotificationService", "TaxCalculator", "ShippingService", "ReportService"
        };

        for (String c : components) {
            graph.addNode(new Node(c, c, NodeType.COMPONENT, "src/main/java/com/example/" + c + ".java"));
        }

        // 2. Add Directed Dependency Edges (A -> B means A depends on B)
        // If B changes, A is impacted (reverse traversal from B reaches A)
        graph.addEdge("AuthService", "UserService", EdgeType.CALLS);
        graph.addEdge("AccountService", "UserService", EdgeType.CALLS);
        graph.addEdge("OrderService", "AccountService", EdgeType.CALLS);
        graph.addEdge("OrderService", "InventoryService", EdgeType.CALLS);
        graph.addEdge("OrderService", "TaxCalculator", EdgeType.CALLS);
        graph.addEdge("ShippingService", "OrderService", EdgeType.CALLS);
        graph.addEdge("ReportService", "OrderService", EdgeType.CALLS);
        graph.addEdge("ReportService", "AccountService", EdgeType.CALLS);
        graph.addEdge("InventoryService", "NotificationService", EdgeType.CALLS);
        graph.addEdge("NotificationService", "AuthService", EdgeType.CALLS);

        // Explicit Dependency Cycle:
        // OrderService -> PaymentGateway -> FraudDetector -> OrderService
        graph.addEdge("OrderService", "PaymentGateway", EdgeType.CALLS);
        graph.addEdge("PaymentGateway", "FraudDetector", EdgeType.CALLS);
        graph.addEdge("FraudDetector", "OrderService", EdgeType.CALLS);

        // Additional edge for PaymentGateway
        graph.addEdge("PaymentGateway", "AuditLogger", EdgeType.CALLS);

        // 3. Define Tests, Costs, and Coverage
        // Unit Tests
        addTest(graph, coverage, costs, "TestAuthUnit", 15, Set.of("AuthService"));
        addTest(graph, coverage, costs, "TestUserUnit", 12, Set.of("UserService"));
        addTest(graph, coverage, costs, "TestAccountUnit", 20, Set.of("AccountService"));
        addTest(graph, coverage, costs, "TestOrderUnit", 30, Set.of("OrderService", "TaxCalculator"));
        addTest(graph, coverage, costs, "TestInventoryUnit", 25, Set.of("InventoryService"));
        addTest(graph, coverage, costs, "TestPaymentUnit", 40, Set.of("PaymentGateway", "AuditLogger"));
        addTest(graph, coverage, costs, "TestFraudUnit", 35, Set.of("FraudDetector"));
        addTest(graph, coverage, costs, "TestAuditUnit", 18, Set.of("AuditLogger"));
        addTest(graph, coverage, costs, "TestNotificationUnit", 22, Set.of("NotificationService"));
        addTest(graph, coverage, costs, "TestTaxUnit", 10, Set.of("TaxCalculator"));
        addTest(graph, coverage, costs, "TestShippingUnit", 28, Set.of("ShippingService"));

        // Integration Tests
        addTest(graph, coverage, costs, "TestIntegrationOrderPayment", 450,
                Set.of("OrderService", "PaymentGateway", "AccountService", "AuditLogger"));
        addTest(graph, coverage, costs, "TestIntegrationAuthUser", 320,
                Set.of("AuthService", "UserService", "NotificationService"));
        addTest(graph, coverage, costs, "TestIntegrationFraudCycle", 380,
                Set.of("PaymentGateway", "FraudDetector", "OrderService"));

        // End-to-End Tests
        addTest(graph, coverage, costs, "TestEndToEndCheckout", 1800,
                Set.of("OrderService", "PaymentGateway", "AccountService", "InventoryService",
                        "TaxCalculator", "ShippingService", "NotificationService", "AuditLogger"));
        addTest(graph, coverage, costs, "TestEndToEndUserLifecycle", 1200,
                Set.of("AuthService", "UserService", "AccountService", "NotificationService", "AuditLogger"));

        // 4. Generate Deterministic Historical Execution Records (using fixed seed PRNG)
        Random rng = new Random(SEED);
        Instant now = Instant.now();

        // Pass rate targets for tests:
        Map<String, Double> passRateMap = Map.ofEntries(
                Map.entry("TestAuthUnit", 0.95),
                Map.entry("TestUserUnit", 0.92),
                Map.entry("TestAccountUnit", 0.88),
                Map.entry("TestOrderUnit", 0.84),
                Map.entry("TestInventoryUnit", 0.90),
                Map.entry("TestPaymentUnit", 0.78), // Frequent failure test
                Map.entry("TestFraudUnit", 0.85),
                Map.entry("TestAuditUnit", 0.98),
                Map.entry("TestNotificationUnit", 0.92),
                Map.entry("TestTaxUnit", 0.96),
                Map.entry("TestShippingUnit", 0.89),
                Map.entry("TestIntegrationOrderPayment", 0.72), // Fragile integration test
                Map.entry("TestIntegrationAuthUser", 0.80),
                Map.entry("TestIntegrationFraudCycle", 0.68),
                Map.entry("TestEndToEndCheckout", 0.65), // Flaky/fragile heavy test
                Map.entry("TestEndToEndUserLifecycle", 0.75)
        );

        for (String testId : costs.keySet()) {
            double targetPassRate = passRateMap.getOrDefault(testId, 0.90);
            long baseCost = costs.get(testId);
            int historyRuns = 15; // 15 historical runs per test

            for (int r = 0; r < historyRuns; r++) {
                boolean passed = rng.nextDouble() < targetPassRate;
                // Add slight jitter to duration: +/- 10%
                long jitter = (long) ((rng.nextDouble() * 0.2 - 0.1) * baseCost);
                long runDuration = Math.max(1, baseCost + jitter);
                Instant runTime = now.minus(historyRuns - r, ChronoUnit.HOURS);

                history.addRecord(new TestHistoryRecord(
                        testId,
                        "build-" + (100 + r),
                        runTime,
                        passed,
                        runDuration,
                        passed ? null : "Assertion failed in " + testId
                ));
            }
        }

        // 5. Define Scenarios with Injected Faults and Ground Truth
        List<Scenario> scenarios = new ArrayList<>();

        // Scenario 1: Small leaf change (TaxCalculator modified)
        scenarios.add(new Scenario(
                "SCN_01_LEAF_TAX",
                "Leaf Component Change (TaxCalculator)",
                "Modification to tax calculation logic affecting order calculations.",
                Set.of("TaxCalculator"),
                List.of(
                        new InjectedFault("FAULT_TAX_RATE", "TaxCalculator", "Incorrect VAT computation for EU jurisdiction",
                                Set.of("TestTaxUnit", "TestOrderUnit", "TestEndToEndCheckout"))
                )
        ));

        // Scenario 2: Medium component change with cycle propagation (PaymentGateway modified)
        scenarios.add(new Scenario(
                "SCN_02_MED_PAYMENT",
                "Medium Change with Cyclic Dependency (PaymentGateway)",
                "PaymentGateway refactor impacting fraud check, order pipeline, and shipping.",
                Set.of("PaymentGateway"),
                List.of(
                        new InjectedFault("FAULT_GATEWAY_TIMEOUT", "PaymentGateway", "Uncaught socket timeout on payment settlement",
                                Set.of("TestPaymentUnit", "TestIntegrationOrderPayment", "TestEndToEndCheckout")),
                        new InjectedFault("FAULT_FRAUD_INTEROP", "PaymentGateway", "Serialization mismatch when communicating with FraudDetector",
                                Set.of("TestIntegrationFraudCycle", "TestEndToEndCheckout"))
                )
        ));

        // Scenario 3: Core shared component change (AccountService modified)
        scenarios.add(new Scenario(
                "SCN_03_CORE_ACCOUNT",
                "Core Shared Component Change (AccountService)",
                "Account limit validation update affecting order, fraud detection, and financial reporting.",
                Set.of("AccountService"),
                List.of(
                        new InjectedFault("FAULT_ACCOUNT_OVERDRAFT", "AccountService", "Off-by-one balance check allowing unauthorized overdraft",
                                Set.of("TestAccountUnit", "TestIntegrationOrderPayment", "TestEndToEndCheckout", "TestEndToEndUserLifecycle"))
                )
        ));

        // Scenario 4: Change inside the dependency cycle (FraudDetector modified)
        scenarios.add(new Scenario(
                "SCN_04_CYCLE_FRAUD",
                "Cycle Node Change (FraudDetector)",
                "Risk threshold tuning in FraudDetector verifying cycle handling and mutual reachability.",
                Set.of("FraudDetector"),
                List.of(
                        new InjectedFault("FAULT_FRAUD_FALSE_POSITIVE", "FraudDetector", "Threshold inversion triggering false positives on valid orders",
                                Set.of("TestFraudUnit", "TestIntegrationFraudCycle", "TestEndToEndCheckout"))
                )
        ));

        // Scenario 5: Multi-point broad change (AuthService and InventoryService modified)
        scenarios.add(new Scenario(
                "SCN_05_BROAD_MULTI",
                "Multi-Origin Broad Change (AuthService + InventoryService)",
                "Concurrent changes across authentication and warehouse inventory pipelines.",
                Set.of("AuthService", "InventoryService"),
                List.of(
                        new InjectedFault("FAULT_AUTH_EXPIRY", "AuthService", "Token expiry timestamp drift on multi-node auth",
                                Set.of("TestAuthUnit", "TestIntegrationAuthUser", "TestEndToEndUserLifecycle")),
                        new InjectedFault("FAULT_INVENTORY_LOCK", "InventoryService", "Deadlock on reservation lock release under load",
                                Set.of("TestInventoryUnit", "TestEndToEndCheckout"))
                )
        ));

        return new BenchmarkSuite(graph, coverage, history, scenarios, costs);
    }

    private static void addTest(
            DependencyGraph graph,
            CoverageRepository coverage,
            Map<String, Long> costs,
            String testId,
            long costMs,
            Set<String> exercisedComponents) {

        graph.addNode(new Node(testId, testId, NodeType.TEST, "src/test/java/com/example/" + testId + ".java"));
        costs.put(testId, costMs);

        for (String c : exercisedComponents) {
            coverage.addCoverage(testId, c, CoverageSource.SYNTHETIC);
            graph.addEdge(testId, c, EdgeType.EXERCISES);
        }
    }
}
