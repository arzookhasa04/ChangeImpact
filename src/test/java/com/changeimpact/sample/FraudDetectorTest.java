package com.changeimpact.sample;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FraudDetectorTest {

    @Test
    @DisplayName("Should flag transaction exceeding threshold as suspicious")
    public void testHighValueTransaction() {
        FraudDetector detector = new FraudDetector(500.0);
        assertTrue(detector.isSuspicious(600.0, "CA"));
    }

    @Test
    @DisplayName("Should flag high-risk zone transaction")
    public void testHighRiskZone() {
        FraudDetector detector = new FraudDetector(5000.0);
        assertTrue(detector.isSuspicious(100.0, "HIGH_RISK_ZONE"));
    }

    @Test
    @DisplayName("Should approve normal transaction")
    public void testNormalTransaction() {
        FraudDetector detector = new FraudDetector(5000.0);
        assertFalse(detector.isSuspicious(100.0, "US"));
    }
}
