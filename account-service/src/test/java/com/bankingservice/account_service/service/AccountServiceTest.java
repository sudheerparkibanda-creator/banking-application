package com.bankingservice.account_service.service;

import com.bankingservice.account_service.dto.CustomerDTO;
import com.bankingservice.account_service.entity.Account;
import com.bankingservice.account_service.exception.CustomerNotFoundException;
import com.bankingservice.account_service.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private RestTemplate restTemplate;

    @SuppressWarnings("rawtypes")
    @Mock
    private CircuitBreakerFactory circuitBreakerFactory;

    @Mock
    private CircuitBreaker circuitBreaker;

    @InjectMocks
    private AccountService accountService;

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
    void createAccountShouldSaveWhenCustomerExists() {
        Account account = new Account(1L, 1000.0, "SAVINGS");
        Account savedAccount = new Account(1L, 1000.0, "SAVINGS");
        savedAccount.setId(10L);

        when(restTemplate.getForObject("http://customer-service/customers/1", CustomerDTO.class))
                .thenReturn(new CustomerDTO(1L, "John", "john@example.com", "9999999999", "Address"));
        when(accountRepository.save(account)).thenReturn(savedAccount);

        Account result = accountService.createAccount(account);

        assertEquals(10L, result.getId());
        verify(restTemplate).getForObject("http://customer-service/customers/1", CustomerDTO.class);
        verify(accountRepository).save(account);
    }

    @Test
    void createAccountShouldThrowWhenCustomerDoesNotExist() {
        Account account = new Account(99L, 1000.0, "SAVINGS");

        when(restTemplate.getForObject("http://customer-service/customers/99", CustomerDTO.class))
                .thenThrow(HttpClientErrorException.NotFound.class);

        assertThrows(CustomerNotFoundException.class, () -> accountService.createAccount(account));

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void createAccountShouldThrowWhenCustomerIdMissing() {
        Account account = new Account(null, 1000.0, "SAVINGS");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> accountService.createAccount(account));

        assertEquals("Customer ID is required to create an account", exception.getMessage());
        verify(restTemplate, never()).getForObject(any(String.class), eq(CustomerDTO.class));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void addMoneyShouldThrowWhenAccountBelongsToDifferentCustomer() {
        Account account = new Account(2L, 1000.0, "SAVINGS");
        account.setId(10L);

        when(restTemplate.getForObject("http://customer-service/customers/1", CustomerDTO.class))
                .thenReturn(new CustomerDTO(1L, "John", "john@example.com", "9999999999", "Address"));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> accountService.addMoney(10L, 100.0, 1L));

        assertEquals("Account 10 does not belong to customer 1", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void withdrawMoneyShouldThrowWhenAccountBelongsToDifferentCustomer() {
        Account account = new Account(2L, 1000.0, "SAVINGS");
        account.setId(11L);

        when(restTemplate.getForObject("http://customer-service/customers/1", CustomerDTO.class))
                .thenReturn(new CustomerDTO(1L, "John", "john@example.com", "9999999999", "Address"));
        when(accountRepository.findById(11L)).thenReturn(Optional.of(account));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> accountService.withdrawMoney(11L, 100.0, 1L));

        assertEquals("Account 11 does not belong to customer 1", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void addMoneyShouldSucceedWhenAccountBelongsToCustomer() {
        Account account = new Account(1L, 1000.0, "SAVINGS");
        account.setId(12L);

        when(restTemplate.getForObject("http://customer-service/customers/1", CustomerDTO.class))
                .thenReturn(new CustomerDTO(1L, "John", "john@example.com", "9999999999", "Address"));
        when(accountRepository.findById(12L)).thenReturn(Optional.of(account));

        boolean result = accountService.addMoney(12L, 250.0, 1L);

        assertTrue(result);
        assertEquals(1250.0, account.getBalance());
        verify(accountRepository).save(account);
    }

    @Test
    void createAccountShouldThrowCustomerNotFoundWhenCustomerServiceIsUnavailable() {
        Account account = new Account(1L, 1000.0, "SAVINGS");

        when(restTemplate.getForObject("http://customer-service/customers/1", CustomerDTO.class))
                .thenThrow(new RuntimeException("Connection refused"));

        assertThrows(CustomerNotFoundException.class, () -> accountService.createAccount(account));

        verify(accountRepository, never()).save(any(Account.class));
    }
}
