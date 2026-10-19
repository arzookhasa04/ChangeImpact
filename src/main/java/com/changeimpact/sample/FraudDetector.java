package com.changeimpact.sample;

public class FraudDetector {
    private final double maxTransactionThreshold;

    public FraudDetector(double maxTransactionThreshold) {
        this.maxTransactionThreshold = maxTransactionThreshold;
    }

    public FraudDetector() {
        this(5000.0);
    }

    public boolean isSuspicious(double amount, String country) {
        if (amount > maxTransactionThreshold) {
            return true;
        }
        return "HIGH_RISK_ZONE".equalsIgnoreCase(country);
    }
}
