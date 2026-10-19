package com.changeimpact.sample;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IntegrationPaymentTest {

    @Test
    @DisplayName("Should execute end-to-end payment workflow across account, fraud, and audit components")
    public void testEndToEndPaymentFlow() {
        AccountService account = new AccountService(500.0);
        FraudDetector fraud = new FraudDetector(2000.0);
        AuditLogger logger = new AuditLogger();
        PaymentGateway gateway = new PaymentGateway(account, fraud, logger);

        // Step 1: Valid payment
        assertTrue(gateway.processPayment(150.0, "DE"));
        assertEquals(350.0, account.getBalance(), 1e-6);

        // Step 2: Fraudulent payment
        assertFalse(gateway.processPayment(2500.0, "DE"));
        assertEquals(350.0, account.getBalance(), 1e-6);

        // Step 3: Insufficient funds
        assertFalse(gateway.processPayment(400.0, "DE"));
        assertEquals(350.0, account.getBalance(), 1e-6);

        // Total log events: 3
        assertEquals(3, logger.getLogs().size());
    }
}
