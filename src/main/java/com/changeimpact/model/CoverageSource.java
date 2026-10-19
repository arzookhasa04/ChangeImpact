package com.changeimpact.model;

/**
 * Indicates whether coverage data is genuinely measured from runtime instrumentation
 * or generated synthetically for benchmark simulation.
 */
public enum CoverageSource {
    MEASURED,
    SYNTHETIC
}
