package com.credlink.report;

import com.credlink.common.exception.ApiException;
import com.credlink.customer.Customer;
import com.credlink.customer.CustomerRepository;
import com.credlink.report.dto.StatementResponse;
import com.credlink.transaction.Transaction;
import com.credlink.transaction.TransactionService;
import com.credlink.transaction.TransactionType;
import com.credlink.transaction.dto.TransactionResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class StatementService {

    private final CustomerRepository customerRepository;
    private final TransactionService transactionService;

    public StatementService(CustomerRepository customerRepository, TransactionService transactionService) {
        this.customerRepository = customerRepository;
        this.transactionService = transactionService;
    }

    public StatementResponse generate(Long merchantId, Long customerId, Instant from, Instant to) {
        Customer customer = customerRepository.findByIdAndMerchantId(customerId, merchantId)
                .orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND", "Customer was not found."));

        transactionService.recalculateCustomerBalance(merchantId, customerId);
        Customer refreshedCustomer = customerRepository.findByIdAndMerchantId(customerId, merchantId).orElse(customer);

        Instant rangeFrom = from != null ? from : Instant.EPOCH;
        Instant rangeTo = to != null ? to : Instant.now().plus(1, ChronoUnit.DAYS);

        List<Transaction> inRange = transactionService.confirmedForCustomerInRange(merchantId, customerId, rangeFrom, rangeTo);

        BigDecimal openingBalance = inRange.isEmpty()
                ? refreshedCustomer.getCurrentBalance()
                : reverseOutTransaction(inRange.get(0));

        BigDecimal totalUdhaar = BigDecimal.ZERO;
        BigDecimal totalJama = BigDecimal.ZERO;
        for (Transaction t : inRange) {
            if (t.getType() == TransactionType.UDHAAR) totalUdhaar = totalUdhaar.add(t.getAmount());
            else totalJama = totalJama.add(t.getAmount());
        }

        BigDecimal closingBalance = inRange.isEmpty() ? refreshedCustomer.getCurrentBalance() : inRange.get(inRange.size() - 1).getBalanceAfter();
        BigDecimal currentOutstanding = refreshedCustomer.getCurrentBalance();

        StatementResponse response = new StatementResponse();
        response.setCustomerId(refreshedCustomer.getId());
        response.setCustomerName(refreshedCustomer.getName());
        response.setFrom(rangeFrom);
        response.setTo(rangeTo);
        response.setOpeningBalance(openingBalance);
        response.setClosingBalance(closingBalance);
        response.setCurrentOutstanding(currentOutstanding);
        response.setTotalUdhaar(totalUdhaar);
        response.setTotalJama(totalJama);
        response.setTotalHistoricalUdhaar(totalUdhaar);
        response.setTotalHistoricalJama(totalJama);
        response.setTransactions(inRange.stream().map(TransactionResponse::from).toList());
        return response;
    }

    /** Opening balance = the first in-range transaction's balanceAfter, undone by its own delta. */
    private BigDecimal reverseOutTransaction(Transaction first) {
        return first.getType() == TransactionType.UDHAAR
                ? first.getBalanceAfter().subtract(first.getAmount())
                : first.getBalanceAfter().add(first.getAmount());
    }
}
