package com.changeimpact.optimizer;

/**
 * Regression test selection strategies supported by ChangeImpact.
 */
public enum TestSelectionStrategy {
    FULL_SUITE,
    DEPENDENCY_BASED,
    GREEDY_COST_ONLY,
    GREEDY_FAILURE_ONLY,
    RISK_AWARE_BUDGETED
}
