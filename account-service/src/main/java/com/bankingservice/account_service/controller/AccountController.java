package com.bankingservice.account_service.controller;

import com.bankingservice.account_service.dto.AccountWithCustomerDTO;
import com.bankingservice.account_service.entity.Account;
import com.bankingservice.account_service.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    @Autowired
    private AccountService accountService;

    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody Account account) {
        log.info("Received createAccount request for customerId={} and accountType={}",
                account != null ? account.getCustomerId() : null,
                account != null ? account.getAccountType() : null);
        Account savedAccount = accountService.createAccount(account);
        log.info("Account created successfully with accountId={} for customerId={}", savedAccount.getId(), savedAccount.getCustomerId());
        return new ResponseEntity<>(savedAccount, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/add")
    public ResponseEntity<Void> addMoney(@PathVariable Long id, @RequestParam Double amount, @RequestParam Long customerId) {
        log.info("Received addMoney request for accountId={}, customerId={}, amount={}", id, customerId, amount);
        boolean success = accountService.addMoney(id, amount, customerId);
        if (success) {
            log.info("addMoney completed for accountId={}, customerId={}", id, customerId);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            log.warn("addMoney failed for accountId={}, customerId={}", id, customerId);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<Void> withdrawMoney(@PathVariable Long id, @RequestParam Double amount, @RequestParam Long customerId) {
        log.info("Received withdrawMoney request for accountId={}, customerId={}, amount={}", id, customerId, amount);
        boolean success = accountService.withdrawMoney(id, amount, customerId);
        if (success) {
            log.info("withdrawMoney completed for accountId={}, customerId={}", id, customerId);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            log.warn("withdrawMoney rejected due to business rule for accountId={}, customerId={}", id, customerId);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountWithCustomerDTO> getAccountDetails(@PathVariable Long id) {
        log.info("Received getAccountDetails request for accountId={}", id);
        AccountWithCustomerDTO account = accountService.getAccountDetails(id);
        log.info("Returning account details for accountId={}", id);
        return new ResponseEntity<>(account, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        log.info("Received deleteAccount request for accountId={}", id);
        accountService.deleteAccount(id);
        log.info("Account deleted for accountId={}", id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/deleteByCustomer/{customerId}")
    public ResponseEntity<Void> deleteAccountsByCustomer(@PathVariable Long customerId) {
        log.info("Received deleteAccountsByCustomer request for customerId={}", customerId);
        accountService.deleteAccountsByCustomerId(customerId);
        log.info("Deleted accounts for customerId={}", customerId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
