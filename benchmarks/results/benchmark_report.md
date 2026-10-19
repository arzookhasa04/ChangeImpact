# ChangeImpact Engine: Benchmark Evaluation Report

This report presents experimental results comparing test-selection strategies across deterministic synthetic scenarios.

> **Notice on Limitations**: Test selection reduces execution time by omitting tests deemed irrelevant or low risk under budget. No heuristic guarantees 100% defect detection under partial budgets. Reduced execution time does not prove equivalence to full execution.

## Executive Summary

| Scenario | Strategy | Budget (ms) | Selected / Total | Used Time (ms) | Time Saved (%) | Affected Cov (%) | Faults Caught | Detection Rate (%)
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :---
| SCN_01_LEAF_TAX | FULL_SUITE | 4405 | 16 / 16 | 4405 | 0.0% | 83.3% | 1 / 1 | 100.0% |
| SCN_01_LEAF_TAX | DEPENDENCY_BASED | 2803 | 8 / 16 | 2803 | 36.4% | 83.3% | 1 / 1 | 100.0% |
| SCN_01_LEAF_TAX | GREEDY_COST_ONLY | 661 | 6 / 16 | 529 | 88.0% | 83.3% | 1 / 1 | 100.0% |
| SCN_01_LEAF_TAX | GREEDY_FAILURE_ONLY | 661 | 6 / 16 | 529 | 88.0% | 83.3% | 1 / 1 | 100.0% |
| SCN_01_LEAF_TAX | RISK_AWARE_BUDGETED | 661 | 5 / 16 | 142 | 96.8% | 83.3% | 1 / 1 | 100.0% |
| SCN_01_LEAF_TAX | GREEDY_COST_ONLY | 1322 | 7 / 16 | 985 | 77.6% | 83.3% | 1 / 1 | 100.0% |
| SCN_01_LEAF_TAX | GREEDY_FAILURE_ONLY | 1322 | 7 / 16 | 985 | 77.6% | 83.3% | 1 / 1 | 100.0% |
| SCN_01_LEAF_TAX | RISK_AWARE_BUDGETED | 1322 | 5 / 16 | 142 | 96.8% | 83.3% | 1 / 1 | 100.0% |
| SCN_01_LEAF_TAX | GREEDY_COST_ONLY | 2203 | 7 / 16 | 985 | 77.6% | 83.3% | 1 / 1 | 100.0% |
| SCN_01_LEAF_TAX | GREEDY_FAILURE_ONLY | 2203 | 7 / 16 | 985 | 77.6% | 83.3% | 1 / 1 | 100.0% |
| SCN_01_LEAF_TAX | RISK_AWARE_BUDGETED | 2203 | 5 / 16 | 142 | 96.8% | 83.3% | 1 / 1 | 100.0% |
| SCN_01_LEAF_TAX | GREEDY_COST_ONLY | 3304 | 8 / 16 | 2803 | 36.4% | 83.3% | 1 / 1 | 100.0% |
| SCN_01_LEAF_TAX | GREEDY_FAILURE_ONLY | 3304 | 8 / 16 | 2803 | 36.4% | 83.3% | 1 / 1 | 100.0% |
| SCN_01_LEAF_TAX | RISK_AWARE_BUDGETED | 3304 | 5 / 16 | 142 | 96.8% | 83.3% | 1 / 1 | 100.0% |
| SCN_02_MED_PAYMENT | FULL_SUITE | 4405 | 16 / 16 | 4405 | 0.0% | 80.0% | 2 / 2 | 100.0% |
| SCN_02_MED_PAYMENT | DEPENDENCY_BASED | 2793 | 7 / 16 | 2793 | 36.6% | 80.0% | 2 / 2 | 100.0% |
| SCN_02_MED_PAYMENT | GREEDY_COST_ONLY | 661 | 5 / 16 | 519 | 88.2% | 80.0% | 2 / 2 | 100.0% |
| SCN_02_MED_PAYMENT | GREEDY_FAILURE_ONLY | 661 | 5 / 16 | 519 | 88.2% | 80.0% | 2 / 2 | 100.0% |
| SCN_02_MED_PAYMENT | RISK_AWARE_BUDGETED | 661 | 4 / 16 | 132 | 97.0% | 80.0% | 1 / 2 | 50.0% |
| SCN_02_MED_PAYMENT | GREEDY_COST_ONLY | 1322 | 6 / 16 | 975 | 77.9% | 80.0% | 2 / 2 | 100.0% |
| SCN_02_MED_PAYMENT | GREEDY_FAILURE_ONLY | 1322 | 6 / 16 | 975 | 77.9% | 80.0% | 2 / 2 | 100.0% |
| SCN_02_MED_PAYMENT | RISK_AWARE_BUDGETED | 1322 | 4 / 16 | 132 | 97.0% | 80.0% | 1 / 2 | 50.0% |
| SCN_02_MED_PAYMENT | GREEDY_COST_ONLY | 2203 | 6 / 16 | 975 | 77.9% | 80.0% | 2 / 2 | 100.0% |
| SCN_02_MED_PAYMENT | GREEDY_FAILURE_ONLY | 2203 | 6 / 16 | 975 | 77.9% | 80.0% | 2 / 2 | 100.0% |
| SCN_02_MED_PAYMENT | RISK_AWARE_BUDGETED | 2203 | 4 / 16 | 132 | 97.0% | 80.0% | 1 / 2 | 50.0% |
| SCN_02_MED_PAYMENT | GREEDY_COST_ONLY | 3304 | 7 / 16 | 2793 | 36.6% | 80.0% | 2 / 2 | 100.0% |
| SCN_02_MED_PAYMENT | GREEDY_FAILURE_ONLY | 3304 | 7 / 16 | 2793 | 36.6% | 80.0% | 2 / 2 | 100.0% |
| SCN_02_MED_PAYMENT | RISK_AWARE_BUDGETED | 3304 | 4 / 16 | 132 | 97.0% | 80.0% | 1 / 2 | 50.0% |
| SCN_03_CORE_ACCOUNT | FULL_SUITE | 4405 | 16 / 16 | 4405 | 0.0% | 83.3% | 1 / 1 | 100.0% |
| SCN_03_CORE_ACCOUNT | DEPENDENCY_BASED | 3998 | 9 / 16 | 3998 | 9.2% | 83.3% | 1 / 1 | 100.0% |
| SCN_03_CORE_ACCOUNT | GREEDY_COST_ONLY | 661 | 6 / 16 | 539 | 87.8% | 83.3% | 1 / 1 | 100.0% |
| SCN_03_CORE_ACCOUNT | GREEDY_FAILURE_ONLY | 661 | 6 / 16 | 539 | 87.8% | 83.3% | 1 / 1 | 100.0% |
| SCN_03_CORE_ACCOUNT | RISK_AWARE_BUDGETED | 661 | 5 / 16 | 152 | 96.5% | 83.3% | 1 / 1 | 100.0% |
| SCN_03_CORE_ACCOUNT | GREEDY_COST_ONLY | 1322 | 7 / 16 | 995 | 77.4% | 83.3% | 1 / 1 | 100.0% |
| SCN_03_CORE_ACCOUNT | GREEDY_FAILURE_ONLY | 1322 | 7 / 16 | 995 | 77.4% | 83.3% | 1 / 1 | 100.0% |
| SCN_03_CORE_ACCOUNT | RISK_AWARE_BUDGETED | 1322 | 5 / 16 | 152 | 96.5% | 83.3% | 1 / 1 | 100.0% |
| SCN_03_CORE_ACCOUNT | GREEDY_COST_ONLY | 2203 | 8 / 16 | 2180 | 50.5% | 83.3% | 1 / 1 | 100.0% |
| SCN_03_CORE_ACCOUNT | GREEDY_FAILURE_ONLY | 2203 | 8 / 16 | 2180 | 50.5% | 83.3% | 1 / 1 | 100.0% |
| SCN_03_CORE_ACCOUNT | RISK_AWARE_BUDGETED | 2203 | 5 / 16 | 152 | 96.5% | 83.3% | 1 / 1 | 100.0% |
| SCN_03_CORE_ACCOUNT | GREEDY_COST_ONLY | 3304 | 8 / 16 | 2180 | 50.5% | 83.3% | 1 / 1 | 100.0% |
| SCN_03_CORE_ACCOUNT | GREEDY_FAILURE_ONLY | 3304 | 8 / 16 | 2813 | 36.1% | 83.3% | 1 / 1 | 100.0% |
| SCN_03_CORE_ACCOUNT | RISK_AWARE_BUDGETED | 3304 | 5 / 16 | 152 | 96.5% | 83.3% | 1 / 1 | 100.0% |
| SCN_04_CYCLE_FRAUD | FULL_SUITE | 4405 | 16 / 16 | 4405 | 0.0% | 80.0% | 1 / 1 | 100.0% |
| SCN_04_CYCLE_FRAUD | DEPENDENCY_BASED | 2793 | 7 / 16 | 2793 | 36.6% | 80.0% | 1 / 1 | 100.0% |
| SCN_04_CYCLE_FRAUD | GREEDY_COST_ONLY | 661 | 5 / 16 | 519 | 88.2% | 80.0% | 1 / 1 | 100.0% |
| SCN_04_CYCLE_FRAUD | GREEDY_FAILURE_ONLY | 661 | 5 / 16 | 519 | 88.2% | 80.0% | 1 / 1 | 100.0% |
| SCN_04_CYCLE_FRAUD | RISK_AWARE_BUDGETED | 661 | 4 / 16 | 132 | 97.0% | 80.0% | 1 / 1 | 100.0% |
| SCN_04_CYCLE_FRAUD | GREEDY_COST_ONLY | 1322 | 6 / 16 | 975 | 77.9% | 80.0% | 1 / 1 | 100.0% |
| SCN_04_CYCLE_FRAUD | GREEDY_FAILURE_ONLY | 1322 | 6 / 16 | 975 | 77.9% | 80.0% | 1 / 1 | 100.0% |
| SCN_04_CYCLE_FRAUD | RISK_AWARE_BUDGETED | 1322 | 4 / 16 | 132 | 97.0% | 80.0% | 1 / 1 | 100.0% |
| SCN_04_CYCLE_FRAUD | GREEDY_COST_ONLY | 2203 | 6 / 16 | 975 | 77.9% | 80.0% | 1 / 1 | 100.0% |
| SCN_04_CYCLE_FRAUD | GREEDY_FAILURE_ONLY | 2203 | 6 / 16 | 975 | 77.9% | 80.0% | 1 / 1 | 100.0% |
| SCN_04_CYCLE_FRAUD | RISK_AWARE_BUDGETED | 2203 | 4 / 16 | 132 | 97.0% | 80.0% | 1 / 1 | 100.0% |
| SCN_04_CYCLE_FRAUD | GREEDY_COST_ONLY | 3304 | 7 / 16 | 2793 | 36.6% | 80.0% | 1 / 1 | 100.0% |
| SCN_04_CYCLE_FRAUD | GREEDY_FAILURE_ONLY | 3304 | 7 / 16 | 2793 | 36.6% | 80.0% | 1 / 1 | 100.0% |
| SCN_04_CYCLE_FRAUD | RISK_AWARE_BUDGETED | 3304 | 4 / 16 | 132 | 97.0% | 80.0% | 1 / 1 | 100.0% |
| SCN_05_BROAD_MULTI | FULL_SUITE | 4405 | 16 / 16 | 4405 | 0.0% | 87.5% | 2 / 2 | 100.0% |
| SCN_05_BROAD_MULTI | DEPENDENCY_BASED | 4356 | 12 / 16 | 4356 | 1.1% | 87.5% | 2 / 2 | 100.0% |
| SCN_05_BROAD_MULTI | GREEDY_COST_ONLY | 661 | 8 / 16 | 510 | 88.4% | 87.5% | 2 / 2 | 100.0% |
| SCN_05_BROAD_MULTI | GREEDY_FAILURE_ONLY | 661 | 8 / 16 | 581 | 86.8% | 87.5% | 2 / 2 | 100.0% |
| SCN_05_BROAD_MULTI | RISK_AWARE_BUDGETED | 661 | 7 / 16 | 194 | 95.6% | 87.5% | 2 / 2 | 100.0% |
| SCN_05_BROAD_MULTI | GREEDY_COST_ONLY | 1322 | 9 / 16 | 897 | 79.6% | 87.5% | 2 / 2 | 100.0% |
| SCN_05_BROAD_MULTI | GREEDY_FAILURE_ONLY | 1322 | 9 / 16 | 1037 | 76.5% | 87.5% | 2 / 2 | 100.0% |
| SCN_05_BROAD_MULTI | RISK_AWARE_BUDGETED | 1322 | 7 / 16 | 194 | 95.6% | 87.5% | 2 / 2 | 100.0% |
| SCN_05_BROAD_MULTI | GREEDY_COST_ONLY | 2203 | 10 / 16 | 1353 | 69.3% | 87.5% | 2 / 2 | 100.0% |
| SCN_05_BROAD_MULTI | GREEDY_FAILURE_ONLY | 2203 | 9 / 16 | 2187 | 50.4% | 87.5% | 2 / 2 | 100.0% |
| SCN_05_BROAD_MULTI | RISK_AWARE_BUDGETED | 2203 | 7 / 16 | 194 | 95.6% | 87.5% | 2 / 2 | 100.0% |
| SCN_05_BROAD_MULTI | GREEDY_COST_ONLY | 3304 | 11 / 16 | 2538 | 42.4% | 87.5% | 2 / 2 | 100.0% |
| SCN_05_BROAD_MULTI | GREEDY_FAILURE_ONLY | 3304 | 11 / 16 | 3171 | 28.0% | 87.5% | 2 / 2 | 100.0% |
| SCN_05_BROAD_MULTI | RISK_AWARE_BUDGETED | 3304 | 7 / 16 | 194 | 95.6% | 87.5% | 2 / 2 | 100.0% |

## Analysis of Strategy Trade-offs

1. **Full Suite Execution**: Guaranteed maximum fault detection within known suite, but consumes 100% of the runtime (4,000+ ms).
2. **Dependency-Based Selection**: Drastically reduces runtime when changes are localized (e.g. leaf changes), but has no cost control if impact propagates broadly.
3. **Greedy Cost-Only Baseline**: Rapidly fills budget with cheap unit tests, but risks missing critical integration/system defects.
4. **Greedy Failure-Only Baseline**: Exploits past failure patterns, but suffers from blind spots on newly modified or rarely failing components.
5. **Risk-Aware Budgeted Selection**: Balances impact proximity, failure history, marginal coverage deduplication, and execution cost under hard budget constraints.
