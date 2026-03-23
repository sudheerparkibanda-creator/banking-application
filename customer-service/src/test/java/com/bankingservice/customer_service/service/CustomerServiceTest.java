package com.bankingservice.customer_service.service;

import com.bankingservice.customer_service.entity.Customer;
import com.bankingservice.customer_service.exception.CustomerNotFoundException;
import com.bankingservice.customer_service.exception.DuplicateCustomerEmailException;
import com.bankingservice.customer_service.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private RestTemplate restTemplate;

    @SuppressWarnings("rawtypes")
    @Mock
    private CircuitBreakerFactory circuitBreakerFactory;

    @Mock
    private CircuitBreaker circuitBreaker;

    @InjectMocks
    private CustomerService customerService;

    @SuppressWarnings({"rawtypes", "unchecked"})
    @BeforeEach
    void setUp() {
        lenient().when(circuitBreakerFactory.create(anyString())).thenReturn(circuitBreaker);
        lenient().when(circuitBreaker.run(any(Supplier.class), any(Function.class))).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            Function<Throwable, ?> fallback = invocation.getArgument(1);
            try {
                return supplier.get();
            } catch (Throwable t) {
                return fallback.apply(t);
            }
        });
    }

    @Test
    void deleteCustomerShouldDeleteAccountsBeforeDeletingCustomer() {
        Customer customer = new Customer("John", "john@example.com", "9999999999", "Address");
        customer.setId(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        customerService.deleteCustomer(1L);

        verify(restTemplate).delete("http://account-service/accounts/deleteByCustomer/1");
        verify(customerRepository).deleteById(1L);
    }

    @Test
    void addCustomerShouldThrowWhenEmailAlreadyExists() {
        Customer customer = new Customer("Jane", "John@Example.com ", "9999999999", "Address");
        Customer existingCustomer = new Customer("John", "john@example.com", "8888888888", "Address");
        existingCustomer.setId(1L);

        when(customerRepository.findByEmail("john@example.com")).thenReturn(Optional.of(existingCustomer));

        assertThrows(DuplicateCustomerEmailException.class, () -> customerService.addCustomer(customer));

        verify(customerRepository, never()).save(customer);
    }

    @Test
    void addCustomerShouldNormalizeEmailBeforeSaving() {
        Customer customer = new Customer("Jane", " Jane@Example.com ", "9999999999", "Address");
        Customer savedCustomer = new Customer("Jane", "jane@example.com", "9999999999", "Address");
        savedCustomer.setId(2L);

        when(customerRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
        when(customerRepository.save(customer)).thenReturn(savedCustomer);

        Customer result = customerService.addCustomer(customer);

        assertEquals("jane@example.com", customer.getEmail());
        assertEquals(2L, result.getId());
        verify(customerRepository).save(customer);
    }

    @Test
    void updateCustomerShouldThrowWhenEmailAlreadyUsedByAnotherCustomer() {
        Customer existingCustomer = new Customer("John", "john@example.com", "9999999999", "Address");
        existingCustomer.setId(1L);
        Customer conflictingCustomer = new Customer("Jane", "jane@example.com", "8888888888", "Address");
        conflictingCustomer.setId(2L);
        Customer updateRequest = new Customer("John Updated", " Jane@Example.com ", "7777777777", "New Address");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(conflictingCustomer));

        assertThrows(DuplicateCustomerEmailException.class, () -> customerService.updateCustomer(1L, updateRequest));

        verify(customerRepository, never()).save(existingCustomer);
    }

    @Test
    void deleteCustomerShouldThrowWhenCustomerDoesNotExist() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> customerService.deleteCustomer(99L));

        verify(restTemplate, never()).delete("http://account-service/accounts/deleteByCustomer/99");
        verify(customerRepository, never()).deleteById(99L);
    }

    @Test
    void deleteCustomerShouldNotDeleteCustomerWhenAccountCleanupFails() {
        Customer customer = new Customer("John", "john@example.com", "9999999999", "Address");
        customer.setId(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        org.mockito.Mockito.doThrow(new RestClientException("Account service unavailable"))
                .when(restTemplate)
                .delete("http://account-service/accounts/deleteByCustomer/1");

        assertThrows(RestClientException.class, () -> customerService.deleteCustomer(1L));

        verify(customerRepository, never()).deleteById(1L);
    }

    @Test
    void deleteCustomerShouldThrowCustomerNotFoundWhenCircuitBreakerOpenAndAccountServiceDown() {
        Customer customer = new Customer("John", "john@example.com", "9999999999", "Address");
        customer.setId(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        org.mockito.Mockito.doThrow(new RuntimeException("Circuit breaker open"))
                .when(restTemplate)
                .delete("http://account-service/accounts/deleteByCustomer/1");

        assertThrows(RuntimeException.class, () -> customerService.deleteCustomer(1L));

        verify(customerRepository, never()).deleteById(1L);
    }
}
