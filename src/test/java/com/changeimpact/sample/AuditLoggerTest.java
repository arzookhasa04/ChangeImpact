package com.changeimpact.sample;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuditLoggerTest {

    @Test
    @DisplayName("Should append audit log events with timestamp prefix")
    public void testAuditLogging() {
        AuditLogger logger = new AuditLogger();
        logger.log("User logged in");
        logger.log("Payment initiated");

        assertEquals(2, logger.getLogs().size());
        assertTrue(logger.getLogs().getFirst().contains("User logged in"));
    }
}
