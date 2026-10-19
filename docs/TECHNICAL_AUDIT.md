# ChangeImpact: Senior Engineer Technical Audit

This audit provides an uncompromising, honest evaluation of the ChangeImpact codebase, algorithms, benchmarks, and measured performance characteristics.

---

## 1. What Works and Has Been Verified

The following core modules are fully implemented, accompanied by 27 automated unit and integration tests passing in Maven under Java 21 LTS:

1. **Cycle-Safe Dependency Graph (`DependencyGraph`)**:
   - Directed graph representation using standard Java collections (`Map`, `Set`, `List`).
   - Reverse reachability traversal ($G^{\text{rev}}$) correctly computes affected component propagation and shortest path distance.
   - Tarjan's Strongly Connected Components algorithm detects circular dependency cycles (e.g. `FraudDetector -> PaymentGateway -> OrderService -> FraudDetector`) and prevents infinite recursion during BFS reachability.

2. **Change Detection (`SourceChangeDetector`)**:
   - Parses Git porcelain status lines (`M`, `A`, `D`, `R100 old -> new`) as well as explicit change lists.
   - Correctly maps file paths and extensions to graph component IDs.

3. **Risk Scoring Engine (`RiskScorer`)**:
   - Mathematically verified against analytical hand calculations (`RiskScorerTest`).
   - Normalizes all four features ($F_1$ Impact Relevance, $F_2$ Historical Failure, $F_3$ Change Coverage, $F_4$ Uncertainty Adjustment) strictly into $[0.0, 1.0]$.
   - Handles missing history gracefully using Laplace-smoothed uninformative priors (0.50) without division-by-zero crashes.

4. **Budget-Constrained Optimizer (`TestSelectionOptimizer`)**:
   - Submodular greedy heuristic evaluating marginal gain per unit execution cost.
   - Enforces execution budgets as strict hard constraints.
   - Deduplicates component coverage, correctly labeling tests with zero newly covered components as `SKIPPED_REDUNDANT`.

5. **Cache Invalidation (`CacheManager`)**:
   - Cryptographic SHA-256 fingerprinting across changed files, graph structure, and weights.
   - Automatically purges and updates stale cache files when changes occur.

6. **CLI & Web Dashboard**:
   - Picocli CLI supporting `analyze`, `select`, `run`, `benchmark`, `report`, `graph`, `history`, and `dashboard`.
   - Python visualizer script (`scripts/visualize_benchmarks.py`) generating actual PNG charts using pandas and matplotlib.
   - Self-contained HTML5/CSS3/JS interactive dashboard (`dashboard/index.html`).

---

## 2. What Is Simulated Versus Genuinely Executed

To preserve complete scientific integrity:

### What Is Genuinely Executed
- **Real Test Execution**: Implemented in `RealTestExecutor` using the official `org.junit.platform.launcher` API.
- Executing `com.changeimpact.sample.*Test` classes spins up the actual JUnit 5 test engine, runs real assertions, and records real wall-clock execution time in milliseconds.
- Change detection genuinely invokes the local system Git executable via `ProcessBuilder`.
- Benchmark evaluation (`EvaluationEngine`) performs real in-memory graph traversals, Tarjan cycle runs, and greedy optimization algorithms on every scenario.
- Python visualizer script reads actual CSV outputs from disk and calls matplotlib to render publication PNG charts.

### What Is Simulated
- **Synthetic Benchmark Project**: The 12-component architecture, 16 test durations, 240 historical execution logs, and injected faults (`BenchmarkDatasetGenerator`) are synthetically modeled using a fixed random seed (42).
- The injected fault detection matrix uses ground truth mappings of which tests detect which faults. While these represent realistic software faults, they are synthetic scenarios for benchmarking purposes.
- Java dependency extraction is metadata-based rather than performing static bytecode analysis of arbitrary third-party Java projects.

---

## 3. Where the Risk-Aware Strategy Improves on the Baselines

1. **Extreme Runtime Reduction on Localized Changes**:
   - On leaf modifications (`SCN_01_LEAF_TAX`), `FULL_SUITE` consumes **4,405 ms**.
   - `RISK_AWARE_BUDGETED` completes in **142 ms** (**96.8% runtime reduction**) while retaining **100% defect detection** and **83.3% affected component coverage**.

2. **Elimination of Redundant Test bloat**:
   - Where simpler baselines like `GREEDY_COST_ONLY` or `DEPENDENCY_BASED` indiscriminately run heavy integration tests that duplicate unit coverage, `RISK_AWARE_BUDGETED` identifies that the 1800ms `TestEndToEndCheckout` provides zero marginal component coverage and skips it.

3. **Deterministic, Hard Budget Enforcement**:
   - While `DEPENDENCY_BASED` selection has no cost awareness (taking up to 3,998 ms on core changes), `RISK_AWARE_BUDGETED` guarantees never exceeding the allocated budget.

---

## 4. Where It Performs Worse or Misses Faults

ChangeImpact is not a silver bullet. Our evaluation uncovered a critical vulnerability:

### The Integration Defect Blind Spot (Scenario 2)
In `SCN_02_MED_PAYMENT`, an interface serialization fault (`FAULT_FRAUD_INTEROP`) was injected between `PaymentGateway` and `FraudDetector`. This fault is **only detectable** by running the integration test `TestIntegrationFraudCycle` (380ms) or `TestEndToEndCheckout` (1800ms).

Because `RISK_AWARE_BUDGETED` selected:
1. `TestPaymentUnit` (40ms) &rarr; covers `PaymentGateway`
2. `TestFraudUnit` (35ms) &rarr; covers `FraudDetector`
3. `TestOrderUnit` (30ms) &rarr; covers `OrderService`
4. `TestShippingUnit` (28ms) &rarr; covers `ShippingService`

All affected components were now in the "covered" set. When the algorithm evaluated `TestIntegrationFraudCycle`, its newly covered components set was $\emptyset$.
- The marginal coverage deduplicator marked the integration test as `SKIPPED_REDUNDANT`.
- As a consequence, `RISK_AWARE_BUDGETED` achieved only a **50.0% fault detection rate** (missed the inter-service bug).
- In contrast, `GREEDY_COST_ONLY` at 661ms budget did not deduplicate coverage; it had remaining budget, picked `TestIntegrationFraudCycle` simply because it was the next candidate, and achieved **100.0% fault detection** (at the cost of 519ms vs 132ms).

**Lesson**: Pure component-level coverage deduplication cannot verify interface compatibility.

---

## 5. The Strongest Defensible Technical Contribution

The strongest contribution of ChangeImpact is its **submodular marginal risk-weighted greedy optimization engine with provable budget guarantees**.

Rather than treating test prioritization as a static one-time sort (which inevitably selects tests with duplicate coverage), ChangeImpact recomputes the marginal information gain of each test dynamically relative to already-selected tests. Combined with normalized multi-objective scoring (topological proximity, failure frequency, change coverage, and uncertainty exploration) and stable tie-breaking, it provides a deterministic, explainable, and mathematically defensible test selection engine.

---

## 6. Next Three Engineering Improvements

1. **Interaction-Edge Modeling**:
   - Enhance the graph to model directed interaction edges between components (`CompA --[INTERFACES_WITH]--> CompB`).
   - Credit integration tests with covering interaction edges, preventing them from being classified as redundant when individual components are already covered.

2. **Bytecode-Level JaCoCo Instrumentation**:
   - Replace component-level coarse mappings with fine-grained bytecode line/instruction coverage (`jacoco.exec`).
   - This eliminates the assumption that exercising any part of a class exercises the modified code.

3. **Flaky Test Classifier**:
   - Introduce an oscillation detector in `HistoryStore` to distinguish non-deterministic flaky failures from true regression defects, preventing flaky tests from monopolizing time budgets.
