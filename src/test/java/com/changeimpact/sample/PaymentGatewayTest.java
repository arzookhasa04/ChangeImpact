package com.changeimpact.sample;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentGatewayTest {

    @Test
    @DisplayName("Should process valid payment and log audit record")
    public void testValidPayment() {
        AccountService account = new AccountService(200.0);
        FraudDetector fraud = new FraudDetector(1000.0);
        AuditLogger logger = new AuditLogger();
        PaymentGateway gateway = new PaymentGateway(account, fraud, logger);

        boolean result = gateway.processPayment(50.0, "US");
        assertTrue(result);
        assertEquals(150.0, account.getBalance(), 1e-6);
        assertFalse(logger.getLogs().isEmpty());
    }

    @Test
    @DisplayName("Should reject payment exceeding fraud threshold")
    public void testFraudRejection() {
        AccountService account = new AccountService(10000.0);
        FraudDetector fraud = new FraudDetector(1000.0);
        AuditLogger logger = new AuditLogger();
        PaymentGateway gateway = new PaymentGateway(account, fraud, logger);

        boolean result = gateway.processPayment(2000.0, "US");
        assertFalse(result);
        assertEquals(10000.0, account.getBalance(), 1e-6);
    }
}
