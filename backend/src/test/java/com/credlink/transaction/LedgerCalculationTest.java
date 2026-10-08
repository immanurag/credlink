package com.credlink.transaction;

import com.credlink.customer.Customer;
import com.credlink.customer.CustomerRepository;
import com.credlink.transaction.dto.TransactionPreviewRequest;
import com.credlink.transaction.dto.TransactionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LedgerCalculationTest {

    private TransactionRepository transactionRepository;
    private CustomerRepository customerRepository;
    private ApplicationEventPublisher eventPublisher;
    private TransactionService service;

    private Customer testCustomer;
    private long txIdCounter = 1;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        customerRepository = mock(CustomerRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new TransactionService(transactionRepository, customerRepository, eventPublisher);

        testCustomer = new Customer();
        testCustomer.setId(100L);
        testCustomer.setMerchantId(1L);
        testCustomer.setName("Ledger Test Customer");
        testCustomer.setCurrentBalance(BigDecimal.ZERO);

        when(customerRepository.findByIdAndMerchantId(100L, 1L)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> {
            Transaction t = i.getArgument(0);
            if (t.getId() == null) {
                t.setId(txIdCounter++);
            }
            return t;
        });
    }

    @Test
    @DisplayName("Test 1: Starting Due = 1000, Payment JAMA 300 -> Due = 700")
    void testCase1_jamaSubtractions() {
        // Set initial credit of 1000
        createAndConfirmTx("UDHAAR", new BigDecimal("1000.00"), null, null);
        assertThat(testCustomer.getCurrentBalance()).isEqualByComparingTo("1000.00");

        // Make JAMA 300
        TransactionResponse jamaResp = createAndConfirmTx("JAMA", new BigDecimal("300.00"), null, null);

        assertThat(jamaResp.getBalanceAfter()).isEqualByComparingTo("700.00");
        assertThat(testCustomer.getCurrentBalance()).isEqualByComparingTo("700.00");
    }

    @Test
    @DisplayName("Test 2: Starting Due = 1000, Payment JAMA 1000 -> Due = 0")
    void testCase2_fullPayment() {
        createAndConfirmTx("UDHAAR", new BigDecimal("1000.00"), null, null);
        TransactionResponse jamaResp = createAndConfirmTx("JAMA", new BigDecimal("1000.00"), null, null);

        assertThat(jamaResp.getBalanceAfter()).isEqualByComparingTo("0.00");
        assertThat(testCustomer.getCurrentBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Test 3: Starting Due = 1000, Payment JAMA 1200 -> Due = -200 (Advance 200)")
    void testCase3_overPayment() {
        createAndConfirmTx("UDHAAR", new BigDecimal("1000.00"), null, null);
        TransactionResponse jamaResp = createAndConfirmTx("JAMA", new BigDecimal("1200.00"), null, null);

        assertThat(jamaResp.getBalanceAfter()).isEqualByComparingTo("-200.00");
        assertThat(testCustomer.getCurrentBalance()).isEqualByComparingTo("-200.00");
    }

    @Test
    @DisplayName("Test 4: Starting Due = 500, Credit UDHAAR 1000 -> Due = 1500")
    void testCase4_additionalCredit() {
        createAndConfirmTx("UDHAAR", new BigDecimal("500.00"), null, null);
        TransactionResponse udhaarResp = createAndConfirmTx("UDHAAR", new BigDecimal("1000.00"), null, null);

        assertThat(udhaarResp.getBalanceAfter()).isEqualByComparingTo("1500.00");
        assertThat(testCustomer.getCurrentBalance()).isEqualByComparingTo("1500.00");
    }

    @Test
    @DisplayName("Test 5: Explicit Outstanding - Rahul ne 1000 rupaye jama kiye, 200 baki -> JAMA 1000, Due = 200")
    void testCase5_explicitOutstandingRahul() {
        TransactionResponse jamaResp = createAndConfirmTx(
                "JAMA",
                new BigDecimal("1000.00"),
                new BigDecimal("200.00"),
                "Rahul ne 1000 rupaye jama kiye, 200 baki"
        );

        assertThat(jamaResp.getAmount()).isEqualByComparingTo("1000.00");
        assertThat(jamaResp.getBalanceAfter()).isEqualByComparingTo("200.00");
        assertThat(testCustomer.getCurrentBalance()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("Test 6: Explicit Outstanding - Shivansh ne 5000 rupaye jama kiye, 400 baki -> JAMA 5000, Due = 400")
    void testCase6_explicitOutstandingShivansh() {
        TransactionResponse jamaResp = createAndConfirmTx(
                "JAMA",
                new BigDecimal("5000.00"),
                new BigDecimal("400.00"),
                "Shivansh ne 5000 rupaye jama kiye, 400 baki"
        );

        assertThat(jamaResp.getAmount()).isEqualByComparingTo("5000.00");
        assertThat(jamaResp.getBalanceAfter()).isEqualByComparingTo("400.00");
        assertThat(testCustomer.getCurrentBalance()).isEqualByComparingTo("400.00");
    }

    private TransactionResponse createAndConfirmTx(String type, BigDecimal amount, BigDecimal explicitOutstanding, String transcript) {
        TransactionPreviewRequest req = new TransactionPreviewRequest();
        req.setCustomerId(testCustomer.getId());
        req.setType(type);
        req.setAmount(amount);
        req.setExplicitOutstanding(explicitOutstanding);
        req.setVoiceTranscript(transcript);
        req.setSource("MANUAL");

        TransactionResponse preview = service.preview(1L, req);

        Transaction savedTx = new Transaction();
        savedTx.setId(preview.getId());
        savedTx.setMerchantId(1L);
        savedTx.setCustomerId(testCustomer.getId());
        savedTx.setType(TransactionType.valueOf(type));
        savedTx.setAmount(amount);
        savedTx.setExplicitOutstanding(explicitOutstanding != null ? explicitOutstanding : service.extractExplicitOutstandingFromText(null, transcript));
        savedTx.setStatus(TransactionStatus.PENDING_REVIEW);

        when(transactionRepository.findByIdAndMerchantId(preview.getId(), 1L)).thenReturn(Optional.of(savedTx));

        return service.confirm(1L, preview.getId(), null);
    }
}
