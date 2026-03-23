package com.bankingservice.account_service.entity;

public class Account {

    private Long id;

    private Long customerId;
    private Double balance;
    private String accountType; // e.g., savings, checking

    // Constructors
    public Account() {}

    public Account(Long customerId, Double balance, String accountType) {
        this.customerId = customerId;
        this.balance = balance;
        this.accountType = accountType;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }
}
