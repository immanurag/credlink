package com.credlink.transaction;

import com.credlink.common.exception.ApiException;
import com.credlink.customer.Customer;
import com.credlink.customer.CustomerRepository;
import com.credlink.transaction.dto.TransactionPreviewRequest;
import com.credlink.transaction.dto.TransactionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    private TransactionRepository transactionRepository;
    private CustomerRepository customerRepository;
    private ApplicationEventPublisher eventPublisher;
    private TransactionService service;

    @BeforeEach
    void setup() {
        transactionRepository = mock(TransactionRepository.class);
        customerRepository = mock(CustomerRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new TransactionService(transactionRepository, customerRepository, eventPublisher);

        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            if (t.getId() == null) t.setId(1L);
            return t;
        });
    }

    private Customer customerWithBalance(BigDecimal balance) {
        Customer c = new Customer();
        c.setId(1L);
        c.setMerchantId(10L);
        c.setName("Ramesh");
        c.setCurrentBalance(balance);
        return c;
    }

    @Test
    void udhaarIncreasesBalance() {
        Customer customer = customerWithBalance(BigDecimal.valueOf(100));
        when(customerRepository.findByIdAndMerchantId(1L, 10L)).thenReturn(Optional.of(customer));

        TransactionPreviewRequest req = new TransactionPreviewRequest();
        req.setCustomerId(1L);
        req.setType("UDHAAR");
        req.setAmount(BigDecimal.valueOf(200));

        TransactionResponse preview = service.preview(10L, req);
        assertEquals(BigDecimal.valueOf(300), preview.getBalanceAfter());
    }

    @Test
    void jamaDecreasesBalance() {
        Customer customer = customerWithBalance(BigDecimal.valueOf(500));
        when(customerRepository.findByIdAndMerchantId(1L, 10L)).thenReturn(Optional.of(customer));

        TransactionPreviewRequest req = new TransactionPreviewRequest();
        req.setCustomerId(1L);
        req.setType("JAMA");
        req.setAmount(BigDecimal.valueOf(200));

        TransactionResponse preview = service.preview(10L, req);
        assertEquals(BigDecimal.valueOf(300), preview.getBalanceAfter());
    }

    @Test
    void confirmUpdatesCustomerRunningBalanceAndFiresEvent() {
        Customer customer = customerWithBalance(BigDecimal.valueOf(100));
        when(customerRepository.findByIdAndMerchantId(1L, 10L)).thenReturn(Optional.of(customer));

        Transaction pending = new Transaction();
        pending.setId(5L);
        pending.setMerchantId(10L);
        pending.setCustomerId(1L);
        pending.setType(TransactionType.UDHAAR);
        pending.setAmount(BigDecimal.valueOf(200));
        pending.setStatus(TransactionStatus.PENDING_REVIEW);
        when(transactionRepository.findByIdAndMerchantId(5L, 10L)).thenReturn(Optional.of(pending));

        TransactionResponse result = service.confirm(10L, 5L, null);

        assertEquals("CONFIRMED", result.getStatus());
        assertEquals(BigDecimal.valueOf(300), result.getBalanceAfter());
        assertEquals(BigDecimal.valueOf(300), customer.getCurrentBalance());
        verify(eventPublisher, times(1)).publishEvent(any(TransactionConfirmedEvent.class));
    }

    @Test
    void cannotConfirmAlreadyConfirmedTransaction() {
        Transaction confirmed = new Transaction();
        confirmed.setId(6L);
        confirmed.setStatus(TransactionStatus.CONFIRMED);
        when(transactionRepository.findByIdAndMerchantId(6L, 10L)).thenReturn(Optional.of(confirmed));

        ApiException ex = assertThrows(ApiException.class, () -> service.confirm(10L, 6L, null));
        assertEquals("TRANSACTION_ALREADY_CONFIRMED", ex.getCode());
    }

    @Test
    void rejectsNonPositiveAmount() {
        Customer customer = customerWithBalance(BigDecimal.ZERO);
        when(customerRepository.findByIdAndMerchantId(1L, 10L)).thenReturn(Optional.of(customer));

        TransactionPreviewRequest req = new TransactionPreviewRequest();
        req.setCustomerId(1L);
        req.setType("UDHAAR");
        req.setAmount(BigDecimal.ZERO);

        ApiException ex = assertThrows(ApiException.class, () -> service.preview(10L, req));
        assertEquals("INVALID_AMOUNT", ex.getCode());
    }

    @Test
    void rejectsInvalidTransactionType() {
        Customer customer = customerWithBalance(BigDecimal.ZERO);
        when(customerRepository.findByIdAndMerchantId(1L, 10L)).thenReturn(Optional.of(customer));

        TransactionPreviewRequest req = new TransactionPreviewRequest();
        req.setCustomerId(1L);
        req.setType("BOGUS");
        req.setAmount(BigDecimal.TEN);

        ApiException ex = assertThrows(ApiException.class, () -> service.preview(10L, req));
        assertEquals("INVALID_TRANSACTION_TYPE", ex.getCode());
    }
}
