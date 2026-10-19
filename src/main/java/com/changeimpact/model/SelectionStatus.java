package com.changeimpact.model;

/**
 * Status indicating why a test was selected or skipped.
 */
public enum SelectionStatus {
    SELECTED,
    SKIPPED_BUDGET_EXCEEDED,
    SKIPPED_REDUNDANT,
    SKIPPED_NOT_AFFECTED,
    SKIPPED_ZERO_MARGINAL_GAIN
}
