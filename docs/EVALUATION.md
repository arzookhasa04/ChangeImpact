# ChangeImpact: Empirical Benchmark Evaluation & Findings

This document presents the methodology, baseline definitions, experimental scenarios, and findings from 70 automated evaluation runs on the ChangeImpact benchmark suite.

---

## 1. Experimental Methodology

To evaluate ChangeImpact rigorously without bias or fabricated numbers, we constructed a benchmark dataset modeling a multi-tier banking and e-commerce software pipeline (`BenchmarkDatasetGenerator`):
- **Graph Topology**: 28 total nodes (12 production services, 16 test suites), 50 directed dependency edges.
- **Cycles**: Contains a realistic circular dependency loop (`OrderService <-> PaymentGateway <-> FraudDetector`).
- **Heterogeneous Costs**: Test durations range across two orders of magnitude: from 10ms unit tests to 1800ms end-to-end tests.
- **Historical Data**: 240 simulated test runs across 15 historical builds, modeling realistic pass rates (65% to 98%).
- **Fault Ground Truth**: Specific semantic bugs injected across different architectural layers with verifiable detection capabilities.

### Evaluated Strategies
1. **`FULL_SUITE` (Baseline 1)**: Runs all 16 tests in the test suite without filtering or budgeting.
2. **`DEPENDENCY_BASED` (Baseline 2)**: Selects all tests that exercise any affected component, without ranking or budgeting.
3. **`GREEDY_COST_ONLY` (Baseline 3)**: Orders candidate tests strictly by lowest execution duration first until budget is exhausted.
4. **`GREEDY_FAILURE_ONLY` (Baseline 4)**: Orders candidate tests strictly by historical failure frequency until budget is exhausted.
5. **`RISK_AWARE_BUDGETED` (Proposed)**: Evaluates marginal risk-weighted coverage per unit execution cost, preventing duplicate coverage credit under a strict hard time budget.

### Evaluated Change Scenarios
- **`SCN_01_LEAF_TAX`**: Modification to leaf service `TaxCalculator`.
- **`SCN_02_MED_PAYMENT`**: Modification to `PaymentGateway` traversing the dependency cycle.
- **`SCN_03_CORE_ACCOUNT`**: Modification to core shared service `AccountService`.
- **`SCN_04_CYCLE_FRAUD`**: Modification directly inside the circular dependency loop (`FraudDetector`).
- **`SCN_05_BROAD_MULTI`**: Concurrent changes in two independent subsystems (`AuthService` and `InventoryService`).

---

## 2. Actual Measured Results (Executive Summary)

The following metrics are extracted directly from `benchmarks/results/benchmark_results.csv`:

| Scenario | Strategy | Budget (ms) | Selected / Total | Used Time (ms) | Runtime Saved (%) | Affected Coverage (%) | Faults Caught | Fault Detection Rate (%) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **SCN_01 (Leaf)** | `FULL_SUITE` | 4405 | 16 / 16 | 4405 | 0.0% | 83.3% | 1 / 1 | 100.0% |
| | `DEPENDENCY_BASED` | 2803 | 8 / 16 | 2803 | 36.4% | 83.3% | 1 / 1 | 100.0% |
| | `GREEDY_COST_ONLY` | 661 | 6 / 16 | 529 | 88.0% | 83.3% | 1 / 1 | 100.0% |
| | `GREEDY_FAIL_ONLY` | 661 | 6 / 16 | 529 | 88.0% | 83.3% | 1 / 1 | 100.0% |
| | **`RISK_AWARE`** | **661** | **5 / 16** | **142** | **96.8%** | **83.3%** | **1 / 1** | **100.0%** |
| **SCN_02 (Payment)** | `FULL_SUITE` | 4405 | 16 / 16 | 4405 | 0.0% | 80.0% | 2 / 2 | 100.0% |
| | `DEPENDENCY_BASED` | 2793 | 7 / 16 | 2793 | 36.6% | 80.0% | 2 / 2 | 100.0% |
| | `GREEDY_COST_ONLY` | 661 | 5 / 16 | 519 | 88.2% | 80.0% | 2 / 2 | 100.0% |
| | `GREEDY_FAIL_ONLY` | 661 | 5 / 16 | 519 | 88.2% | 80.0% | 2 / 2 | 100.0% |
| | **`RISK_AWARE`** | **661** | **4 / 16** | **132** | **97.0%** | **80.0%** | **1 / 2** | **50.0%** |
| **SCN_03 (Account)** | `FULL_SUITE` | 4405 | 16 / 16 | 4405 | 0.0% | 83.3% | 1 / 1 | 100.0% |
| | `DEPENDENCY_BASED` | 3998 | 9 / 16 | 3998 | 9.2% | 83.3% | 1 / 1 | 100.0% |
| | `GREEDY_COST_ONLY` | 661 | 6 / 16 | 539 | 87.8% | 83.3% | 1 / 1 | 100.0% |
| | `GREEDY_FAIL_ONLY` | 661 | 6 / 16 | 539 | 87.8% | 83.3% | 1 / 1 | 100.0% |
| | **`RISK_AWARE`** | **661** | **5 / 16** | **152** | **96.5%** | **83.3%** | **1 / 1** | **100.0%** |
| **SCN_04 (Cycle)** | `FULL_SUITE` | 4405 | 16 / 16 | 4405 | 0.0% | 80.0% | 1 / 1 | 100.0% |
| | `DEPENDENCY_BASED` | 2793 | 7 / 16 | 2793 | 36.6% | 80.0% | 1 / 1 | 100.0% |
| | **`RISK_AWARE`** | **661** | **4 / 16** | **132** | **97.0%** | **80.0%** | **1 / 1** | **100.0%** |
| **SCN_05 (Multi)** | `FULL_SUITE` | 4405 | 16 / 16 | 4405 | 0.0% | 90.9% | 2 / 2 | 100.0% |
| | `DEPENDENCY_BASED` | 4405 | 16 / 16 | 4405 | 0.0% | 90.9% | 2 / 2 | 100.0% |
| | **`RISK_AWARE`** | **661** | **6 / 16** | **154** | **96.5%** | **90.9%** | **2 / 2** | **100.0%** |

---

## 3. In-Depth Analysis of Key Findings

### 1. Massive Runtime Reductions via Deduplication
In Scenarios 1, 3, 4, and 5, `RISK_AWARE_BUDGETED` achieves **96.5% to 97.0% execution time reduction** while achieving **100% defect detection** of the injected faults.
- Why? In these scenarios, individual component unit tests (`10ms - 40ms`) fully exercise the affected modules.
- The submodular marginal deduplicator correctly identifies heavy integration tests (`TestEndToEndCheckout`, 1800ms) as providing zero marginal component coverage and skips them, conserving precious CI minutes.

### 2. Critical Miss in Scenario 2: The Integration Defect Trap
In Scenario 2 (`PaymentGateway` change), two faults were injected:
1. `FAULT_GATEWAY_TIMEOUT`: A local socket timeout error detectable by unit test `TestPaymentUnit` (40ms) or integration tests.
2. `FAULT_FRAUD_INTEROP`: A serialization contract mismatch between `PaymentGateway` and `FraudDetector` detectable **only** by `TestIntegrationFraudCycle` (380ms) or `TestEndToEndCheckout` (1800ms).

`RISK_AWARE_BUDGETED` selected:
- `TestPaymentUnit` (covers `PaymentGateway`)
- `TestFraudUnit` (covers `FraudDetector`)
- `TestOrderUnit` (covers `OrderService`)
- `TestShippingUnit` (covers `ShippingService`)
Total time used: **132 ms**.
All affected components were covered! The deduplicator classified `TestIntegrationFraudCycle` as `SKIPPED_REDUNDANT` because both `PaymentGateway` and `FraudDetector` were already covered by the unit tests.
As a result:
- `RISK_AWARE_BUDGETED` caught 1 out of 2 faults (**50% detection rate**).
- `GREEDY_COST_ONLY` (at 661ms budget) selected `TestIntegrationFraudCycle` because it simply filled the remaining budget with the next cheapest candidate (380ms), catching both faults (**100% detection rate**) but using **519 ms** (almost 4x more time).
- `FULL_SUITE` and `DEPENDENCY_BASED` caught both faults, but required **4405 ms** and **2793 ms** respectively.

**Key Scientific Takeaway**: Component-level coverage deduplication cannot distinguish between testing components in isolation versus testing their interaction contracts. When interface bugs exist, greedy deduplication on component sets can cause false omissions.

---

## 4. Computational Overhead

Measured on a standard development machine (Intel 64-bit Windows 11, Java 21 LTS):
- **Graph Traversal & Cycle Detection**: $< 2$ ms.
- **Risk Scoring & Test Ranking**: $< 1$ ms.
- **Budgeted Submodular Optimizer**: $< 2$ ms.
- **Total Decision Latency**: **$< 15$ ms** per run.
- **JVM Heap Memory Delta**: $< 4.5$ MB for full analysis.

The analysis latency is negligible compared to running even a single test.
