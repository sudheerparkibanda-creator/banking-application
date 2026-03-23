package com.bankingservice.account_service.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(Long accountId) {
        super("Account with ID " + accountId + " not found");
    }

    public AccountNotFoundException(String message) {
        super(message);
    }
}

