package com.bankingservice.customer_service.exception;

public class DuplicateCustomerEmailException extends RuntimeException {

    public DuplicateCustomerEmailException(String email) {
        super("Customer email already exists: " + email);
    }
}

