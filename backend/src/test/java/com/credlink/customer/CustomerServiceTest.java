package com.credlink.customer;

import com.credlink.common.exception.ApiException;
import com.credlink.customer.dto.CustomerRequest;
import com.credlink.customer.dto.CustomerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerServiceTest {

    private CustomerRepository repository;
    private CustomerService service;

    @BeforeEach
    void setup() {
        repository = mock(CustomerRepository.class);
        service = new CustomerService(repository);
        when(repository.save(any())).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            if (c.getId() == null) c.setId(1L);
            return c;
        });
    }

    @Test
    void createsCustomerScopedToMerchant() {
        CustomerRequest req = new CustomerRequest();
        req.setName("Ramesh Kumar");
        req.setPhone("+919810244321");

        CustomerResponse response = service.create(10L, req);
        assertEquals("Ramesh Kumar", response.getName());

        verify(repository).save(argThat(c -> c.getMerchantId().equals(10L)));
    }

    @Test
    void throwsNotFoundWhenCustomerBelongsToAnotherMerchant() {
        when(repository.findByIdAndMerchantId(1L, 999L)).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () -> service.get(999L, 1L));
        assertEquals("CUSTOMER_NOT_FOUND", ex.getCode());
    }

    @Test
    void getReturnsCustomerWhenOwnedByMerchant() {
        Customer c = new Customer();
        c.setId(1L);
        c.setMerchantId(10L);
        c.setName("Ramesh");
        when(repository.findByIdAndMerchantId(1L, 10L)).thenReturn(Optional.of(c));

        CustomerResponse response = service.get(10L, 1L);
        assertEquals("Ramesh", response.getName());
    }
}
