package com.changeimpact.scorer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configurable weights for the risk scoring model.
 * Formula: Risk(T) = w1 * ImpactRelevance + w2 * FailureSignal + w3 * ChangeCoverage + w4 * UncertaintyAdjustment
 */
public class RiskWeights {
    private final double w1ImpactRelevance;
    private final double w2FailureSignal;
    private final double w3ChangeCoverage;
    private final double w4UncertaintyAdjustment;

    @JsonCreator
    public RiskWeights(
            @JsonProperty("w1ImpactRelevance") double w1ImpactRelevance,
            @JsonProperty("w2FailureSignal") double w2FailureSignal,
            @JsonProperty("w3ChangeCoverage") double w3ChangeCoverage,
            @JsonProperty("w4UncertaintyAdjustment") double w4UncertaintyAdjustment) {
        if (w1ImpactRelevance < 0 || w2FailureSignal < 0 || w3ChangeCoverage < 0 || w4UncertaintyAdjustment < 0) {
            throw new IllegalArgumentException("Weights must be non-negative");
        }
        double sum = w1ImpactRelevance + w2FailureSignal + w3ChangeCoverage + w4UncertaintyAdjustment;
        if (sum <= 0) {
            throw new IllegalArgumentException("Sum of weights must be positive");
        }
        // Normalize weights so they sum to 1.0
        this.w1ImpactRelevance = w1ImpactRelevance / sum;
        this.w2FailureSignal = w2FailureSignal / sum;
        this.w3ChangeCoverage = w3ChangeCoverage / sum;
        this.w4UncertaintyAdjustment = w4UncertaintyAdjustment / sum;
    }

    public static RiskWeights defaultWeights() {
        return new RiskWeights(0.35, 0.25, 0.25, 0.15);
    }

    public static RiskWeights impactOnly() {
        return new RiskWeights(1.0, 0.0, 0.0, 0.0);
    }

    public static RiskWeights failureOnly() {
        return new RiskWeights(0.0, 1.0, 0.0, 0.0);
    }

    public static RiskWeights coverageOnly() {
        return new RiskWeights(0.0, 0.0, 1.0, 0.0);
    }

    public double getW1ImpactRelevance() {
        return w1ImpactRelevance;
    }

    public double getW2FailureSignal() {
        return w2FailureSignal;
    }

    public double getW3ChangeCoverage() {
        return w3ChangeCoverage;
    }

    public double getW4UncertaintyAdjustment() {
        return w4UncertaintyAdjustment;
    }

    @Override
    public String toString() {
        return String.format("RiskWeights{w1(Impact)=%.2f, w2(Failure)=%.2f, w3(Coverage)=%.2f, w4(Uncertainty)=%.2f}",
                w1ImpactRelevance, w2FailureSignal, w3ChangeCoverage, w4UncertaintyAdjustment);
    }
}
