package com.bankingservice.account_service.service;

import com.bankingservice.account_service.dto.AccountWithCustomerDTO;
import com.bankingservice.account_service.dto.CustomerDTO;
import com.bankingservice.account_service.entity.Account;
import com.bankingservice.account_service.exception.AccountNotFoundException;
import com.bankingservice.account_service.exception.CustomerNotFoundException;
import com.bankingservice.account_service.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RestTemplate restTemplate;

    @SuppressWarnings("rawtypes")
    @Autowired
    private CircuitBreakerFactory circuitBreakerFactory;

    public Account createAccount(Account account) {
        if (account == null || account.getCustomerId() == null) {
            throw new IllegalArgumentException("Customer ID is required to create an account");
        }

        log.info("Creating account for customerId={} and accountType={}", account.getCustomerId(), account.getAccountType());
        validateCustomerExists(account.getCustomerId());
        Account saved = accountRepository.save(account);
        log.info("Created accountId={} for customerId={}", saved.getId(), saved.getCustomerId());
        return saved;
    }

    public boolean addMoney(Long accountId, Double amount, Long customerId) {
        log.info("Adding money for accountId={}, customerId={}, amount={}", accountId, customerId, amount);
        validateCustomerExists(customerId);
        Account account = getAccountOrThrow(accountId);
        validateAccountOwnership(account, customerId);
        account.setBalance(account.getBalance() + amount);
        accountRepository.save(account);
        log.info("Updated balance for accountId={} after addMoney", accountId);
        return true;
    }

    public boolean withdrawMoney(Long accountId, Double amount, Long customerId) {
        log.info("Withdrawing money for accountId={}, customerId={}, amount={}", accountId, customerId, amount);
        validateCustomerExists(customerId);
        Account account = getAccountOrThrow(accountId);
        validateAccountOwnership(account, customerId);
        if (account.getBalance() >= amount) {
            account.setBalance(account.getBalance() - amount);
            accountRepository.save(account);
            log.info("Updated balance for accountId={} after withdrawMoney", accountId);
            return true;
        }
        log.warn("Insufficient balance for withdrawMoney on accountId={}, customerId={}", accountId, customerId);
        return false;
    }

    public AccountWithCustomerDTO getAccountDetails(Long accountId) {
        log.info("Fetching account details for accountId={}", accountId);
        Account account = getAccountOrThrow(accountId);
        CustomerDTO customer = getCustomer(account.getCustomerId());
        return new AccountWithCustomerDTO(account.getId(), account.getBalance(), account.getAccountType(), customer);
    }

    public boolean deleteAccount(Long accountId) {
        log.info("Deleting account for accountId={}", accountId);
        if (accountRepository.existsById(accountId)) {
            accountRepository.deleteById(accountId);
            log.info("Deleted accountId={}", accountId);
            return true;
        }
        throw new AccountNotFoundException(accountId);
    }

    public void deleteAccountsByCustomerId(Long customerId) {
        log.info("Deleting all accounts for customerId={}", customerId);
        List<Account> accountsToDelete = accountRepository.findByCustomerId(customerId);
        accountRepository.deleteAll(accountsToDelete);
        log.info("Deleted {} accounts for customerId={}", accountsToDelete.size(), customerId);
    }

    private void validateCustomerExists(Long customerId) {
        log.debug("Validating customer existence for customerId={}", customerId);
        getCustomer(customerId);
    }

    private void validateAccountOwnership(Account account, Long customerId) {
        if (!Objects.equals(account.getCustomerId(), customerId)) {
            log.warn("Ownership validation failed for accountId={}, expectedCustomerId={}, actualCustomerId={}",
                    account.getId(), customerId, account.getCustomerId());
            throw new IllegalArgumentException("Account " + account.getId() + " does not belong to customer " + customerId);
        }
    }

    private Account getAccountOrThrow(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @SuppressWarnings("unchecked")
    private CustomerDTO getCustomer(Long customerId) {
        log.debug("Calling customer-service to fetch customerId={}", customerId);
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("customer-service");
        return (CustomerDTO) circuitBreaker.run(
                () -> {
                    try {
                        CustomerDTO customer = restTemplate.getForObject(
                                "http://CUSTOMER-SERVICE/customers/" + customerId, CustomerDTO.class);
                        if (customer == null) {
                            log.warn("customer-service returned empty customer for customerId={}", customerId);
                            throw new CustomerNotFoundException(customerId);
                        }
                        return customer;
                    } catch (HttpClientErrorException.NotFound e) {
                        log.warn("customer-service returned not found for customerId={}", customerId);
                        throw new CustomerNotFoundException(customerId);
                    }
                },
                throwable -> {
                    log.warn("Circuit breaker fallback triggered for customerId={}: {}", customerId, throwable.getMessage());
                    if (throwable instanceof CustomerNotFoundException) {
                        throw (CustomerNotFoundException) throwable;
                    }
                    throw new CustomerNotFoundException(customerId);
                }
        );
    }
}
