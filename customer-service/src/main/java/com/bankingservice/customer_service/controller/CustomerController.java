package com.bankingservice.customer_service.controller;

import com.bankingservice.customer_service.entity.Customer;
import com.bankingservice.customer_service.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    @Autowired
    private CustomerService customerService;

    @PostMapping
    public ResponseEntity<Customer> addCustomer(@RequestBody Customer customer) {
        log.info("Received addCustomer request for email={}", customer != null ? customer.getEmail() : null);
        Customer savedCustomer = customerService.addCustomer(customer);
        log.info("Customer created successfully with customerId={}", savedCustomer.getId());
        return new ResponseEntity<>(savedCustomer, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers() {
        log.info("Received getAllCustomers request");
        List<Customer> customers = customerService.getAllCustomers();
        log.info("Returning {} customers", customers.size());
        return new ResponseEntity<>(customers, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable Long id) {
        log.info("Received getCustomerById request for customerId={}", id);
        Optional<Customer> customer = customerService.getCustomerById(id);
        if (customer.isPresent()) {
            log.info("Customer found for customerId={}", id);
            return new ResponseEntity<>(customer.get(), HttpStatus.OK);
        } else {
            log.warn("Customer not found for customerId={}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(@PathVariable Long id, @RequestBody Customer customerDetails) {
        log.info("Received updateCustomer request for customerId={}", id);
        Customer updatedCustomer = customerService.updateCustomer(id, customerDetails);
        if (updatedCustomer != null) {
            log.info("Customer updated for customerId={}", id);
            return new ResponseEntity<>(updatedCustomer, HttpStatus.OK);
        } else {
            log.warn("Update returned empty result for customerId={}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        log.info("Received deleteCustomer request for customerId={}", id);
        customerService.deleteCustomer(id);
        log.info("Customer deleted for customerId={}", id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
