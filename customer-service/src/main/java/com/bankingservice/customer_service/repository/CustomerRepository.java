package com.bankingservice.customer_service.repository;

import com.bankingservice.customer_service.entity.Customer;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class CustomerRepository {

    private final List<Customer> customers = new ArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    public Customer save(Customer customer) {
        if (customer.getId() == null) {
            customer.setId(idCounter.getAndIncrement());
            customers.add(customer);
        } else {
            // Update existing customer
            Optional<Customer> existing = findById(customer.getId());
            if (existing.isPresent()) {
                int index = customers.indexOf(existing.get());
                customers.set(index, customer);
            } else {
                customers.add(customer);
            }
        }
        return customer;
    }

    public List<Customer> findAll() {
        return new ArrayList<>(customers);
    }

    public Optional<Customer> findById(Long id) {
        return customers.stream().filter(c -> c.getId().equals(id)).findFirst();
    }

    public Optional<Customer> findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        return customers.stream()
                .filter(customer -> normalizeEmail(customer.getEmail()).equals(normalizedEmail))
                .findFirst();
    }

    public boolean existsById(Long id) {
        return customers.stream().anyMatch(c -> c.getId().equals(id));
    }

    public void deleteById(Long id) {
        customers.removeIf(c -> c.getId().equals(id));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
