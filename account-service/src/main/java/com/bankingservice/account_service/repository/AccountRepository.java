package com.bankingservice.account_service.repository;

import com.bankingservice.account_service.entity.Account;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class AccountRepository {

    private final List<Account> accounts = new ArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    public Account save(Account account) {
        if (account.getId() == null) {
            account.setId(idCounter.getAndIncrement());
            accounts.add(account);
        } else {
            // Update existing account
            Optional<Account> existing = findById(account.getId());
            if (existing.isPresent()) {
                int index = accounts.indexOf(existing.get());
                accounts.set(index, account);
            } else {
                accounts.add(account);
            }
        }
        return account;
    }

    public List<Account> findAll() {
        return new ArrayList<>(accounts);
    }

    public Optional<Account> findById(Long id) {
        return accounts.stream().filter(a -> a.getId().equals(id)).findFirst();
    }

    public List<Account> findByCustomerId(Long customerId) {
        return accounts.stream()
                .filter(a -> a.getCustomerId().equals(customerId))
                .collect(Collectors.toList());
    }

    public boolean existsById(Long id) {
        return accounts.stream().anyMatch(a -> a.getId().equals(id));
    }

    public void deleteById(Long id) {
        accounts.removeIf(a -> a.getId().equals(id));
    }

    public void deleteAll(List<Account> accountsToDelete) {
        accounts.removeAll(accountsToDelete);
    }
}
