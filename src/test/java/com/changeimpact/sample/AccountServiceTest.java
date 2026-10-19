package com.changeimpact.sample;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AccountServiceTest {

    @Test
    @DisplayName("Should successfully debit account when funds are sufficient")
    public void testDebitSuccess() {
        AccountService account = new AccountService(100.0);
        assertTrue(account.debit(40.0));
        assertEquals(60.0, account.getBalance(), 1e-6);
    }

    @Test
    @DisplayName("Should reject debit when funds are insufficient")
    public void testDebitInsufficientFunds() {
        AccountService account = new AccountService(50.0);
        assertFalse(account.debit(100.0));
        assertEquals(50.0, account.getBalance(), 1e-6);
    }

    @Test
    @DisplayName("Should credit account correctly")
    public void testCredit() {
        AccountService account = new AccountService(50.0);
        account.credit(25.0);
        assertEquals(75.0, account.getBalance(), 1e-6);
    }
}
