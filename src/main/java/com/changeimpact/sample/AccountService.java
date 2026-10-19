package com.changeimpact.sample;

public class AccountService {
    private double balance;

    public AccountService(double initialBalance) {
        this.balance = initialBalance;
    }

    public AccountService() {
        this(1000.0);
    }

    public boolean debit(double amount) {
        if (amount <= 0 || balance < amount) {
            return false;
        }
        balance -= amount;
        return true;
    }

    public void credit(double amount) {
        if (amount > 0) {
            balance += amount;
        }
    }

    public double getBalance() {
        return balance;
    }
}
