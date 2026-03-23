package com.bankingservice.account_service.dto;

public class AccountWithCustomerDTO {
    private Long id;
    private Double balance;
    private String accountType;
    private CustomerDTO customer;

    // Constructors
    public AccountWithCustomerDTO() {}

    public AccountWithCustomerDTO(Long id, Double balance, String accountType, CustomerDTO customer) {
        this.id = id;
        this.balance = balance;
        this.accountType = accountType;
        this.customer = customer;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public CustomerDTO getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerDTO customer) {
        this.customer = customer;
    }
}
