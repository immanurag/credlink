package com.credlink.dashboard;

import com.credlink.customer.Customer;
import com.credlink.customer.CustomerRepository;
import com.credlink.dashboard.dto.DashboardSummaryResponse;
import com.credlink.dashboard.dto.TrendPoint;
import com.credlink.transaction.Transaction;
import com.credlink.transaction.TransactionService;
import com.credlink.transaction.TransactionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final TransactionService transactionService;

    public DashboardService(CustomerRepository customerRepository, TransactionService transactionService) {
        this.customerRepository = customerRepository;
        this.transactionService = transactionService;
    }

    public DashboardSummaryResponse summary(Long merchantId) {
        List<Customer> customers = customerRepository.findByMerchantId(merchantId);

        BigDecimal totalLenaHai = BigDecimal.ZERO;
        BigDecimal totalDenaHai = BigDecimal.ZERO;
        int newThisWeek = 0;
        Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);

        for (Customer c : customers) {
            if (c.getCurrentBalance() != null && c.getCurrentBalance().compareTo(BigDecimal.ZERO) > 0) {
                totalLenaHai = totalLenaHai.add(c.getCurrentBalance());
            } else if (c.getCurrentBalance() != null && c.getCurrentBalance().compareTo(BigDecimal.ZERO) < 0) {
                totalDenaHai = totalDenaHai.add(c.getCurrentBalance().abs());
            }
            if (c.getCreatedAt() != null && c.getCreatedAt().isAfter(weekAgo)) newThisWeek++;
        }

        Instant startOfToday = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant();
        List<Transaction> todaysTx = transactionService.confirmedInRange(merchantId, startOfToday, Instant.now());

        BigDecimal todaysJama = BigDecimal.ZERO;
        BigDecimal todaysUdhaar = BigDecimal.ZERO;
        for (Transaction t : todaysTx) {
            if (t.getAmount() != null) {
                if (t.getType() == TransactionType.JAMA) todaysJama = todaysJama.add(t.getAmount());
                else todaysUdhaar = todaysUdhaar.add(t.getAmount());
            }
        }

        DashboardSummaryResponse response = new DashboardSummaryResponse();
        response.setTotalLenaHai(totalLenaHai);
        response.setTotalDenaHai(totalDenaHai);
        response.setTodaysJama(todaysJama);
        response.setTodaysUdhaar(todaysUdhaar);
        response.setTodaysTransactionCount(todaysTx.size());
        response.setActiveCustomerCount(customers.size());
        response.setNewCustomersThisWeek(newThisWeek);
        return response;
    }

    public List<TrendPoint> trend(Long merchantId, int days) {
        List<TrendPoint> points = new ArrayList<>();
        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("EEE");
        ZoneId zone = ZoneId.systemDefault();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = LocalDate.now(zone).minusDays(i);
            Instant from = day.atStartOfDay(zone).toInstant();
            Instant to = day.plusDays(1).atStartOfDay(zone).toInstant();

            List<Transaction> dayTx = transactionService.confirmedInRange(merchantId, from, to);
            BigDecimal udhaar = BigDecimal.ZERO;
            BigDecimal jama = BigDecimal.ZERO;
            for (Transaction t : dayTx) {
                if (t.getAmount() != null) {
                    if (t.getType() == TransactionType.UDHAAR) udhaar = udhaar.add(t.getAmount());
                    else jama = jama.add(t.getAmount());
                }
            }
            points.add(new TrendPoint(labelFmt.format(day), udhaar, jama));
        }
        return points;
    }
}
