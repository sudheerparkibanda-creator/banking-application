package com.bankingservice.customer_service.service;

import com.bankingservice.customer_service.entity.Customer;
import com.bankingservice.customer_service.exception.CustomerNotFoundException;
import com.bankingservice.customer_service.exception.DuplicateCustomerEmailException;
import com.bankingservice.customer_service.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RestTemplate restTemplate;

    @SuppressWarnings("rawtypes")
    @Autowired
    private CircuitBreakerFactory circuitBreakerFactory;

    public Customer addCustomer(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer details are required");
        }

        String normalizedEmail = validateAndNormalizeEmail(customer.getEmail(), null);
        customer.setEmail(normalizedEmail);
        log.info("Creating customer for email={}", normalizedEmail);
        Customer saved = customerRepository.save(customer);
        log.info("Created customerId={}", saved.getId());
        return saved;
    }

    public List<Customer> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();
        log.debug("Fetched {} customers", customers.size());
        return customers;
    }

    public Optional<Customer> getCustomerById(Long id) {
        Optional<Customer> customer = customerRepository.findById(id);
        if (customer.isPresent()) {
            log.debug("Customer found for customerId={}", id);
        } else {
            log.warn("Customer lookup failed for customerId={}", id);
        }
        return customer;
    }

    public Customer updateCustomer(Long id, Customer customerDetails) {
        log.info("Updating customerId={}", id);
        Optional<Customer> optionalCustomer = customerRepository.findById(id);
        if (optionalCustomer.isPresent()) {
            if (customerDetails == null) {
                throw new IllegalArgumentException("Customer details are required");
            }

            Customer customer = optionalCustomer.get();
            String normalizedEmail = validateAndNormalizeEmail(customerDetails.getEmail(), id);
            customer.setName(customerDetails.getName());
            customer.setEmail(normalizedEmail);
            customer.setPhone(customerDetails.getPhone());
            customer.setAddress(customerDetails.getAddress());
            Customer updated = customerRepository.save(customer);
            log.info("Updated customerId={}", id);
            return updated;
        }
        log.warn("Cannot update missing customerId={}", id);
        throw new CustomerNotFoundException(id);
    }

    @SuppressWarnings("unchecked")
    public boolean deleteCustomer(Long id) {
        log.info("Deleting customerId={} and linked accounts", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        log.info("Calling account-service to delete linked accounts for customerId={}", id);
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("account-service");
        circuitBreaker.run(
                () -> {
                    restTemplate.delete("http://ACCOUNT-SERVICE/accounts/deleteByCustomer/" + id);
                    return null;
                },
                throwable -> {
                    log.error("Circuit breaker fallback: failed to delete accounts for customerId={}", id, throwable);
                    if (throwable instanceof RuntimeException) {
                        throw (RuntimeException) throwable;
                    }
                    throw new RuntimeException("Account service unavailable for customerId: " + id, throwable);
                }
        );

        customerRepository.deleteById(customer.getId());
        log.info("Deleted customerId={} successfully", id);
        return true;
    }

    private String validateAndNormalizeEmail(String email, Long currentCustomerId) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer email is required");
        }

        String normalizedEmail = normalizeEmail(email);
        Optional<Customer> existingCustomer = customerRepository.findByEmail(normalizedEmail);
        if (existingCustomer.isPresent() && !existingCustomer.get().getId().equals(currentCustomerId)) {
            log.warn("Duplicate email validation failed for email={}", normalizedEmail);
            throw new DuplicateCustomerEmailException(normalizedEmail);
        }

        return normalizedEmail;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
