package com.changeimpact.sample;

public class PaymentGateway {
    private final AccountService accountService;
    private final FraudDetector fraudDetector;
    private final AuditLogger auditLogger;

    public PaymentGateway(AccountService accountService, FraudDetector fraudDetector, AuditLogger auditLogger) {
        this.accountService = accountService;
        this.fraudDetector = fraudDetector;
        this.auditLogger = auditLogger;
    }

    public boolean processPayment(double amount, String country) {
        if (fraudDetector.isSuspicious(amount, country)) {
            auditLogger.log("Payment rejected due to suspected fraud: " + amount);
            return false;
        }
        boolean success = accountService.debit(amount);
        if (success) {
            auditLogger.log("Payment processed successfully: " + amount);
        } else {
            auditLogger.log("Payment failed due to insufficient funds: " + amount);
        }
        return success;
    }
}
