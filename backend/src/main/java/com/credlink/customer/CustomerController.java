package com.credlink.customer;

import com.credlink.customer.dto.CustomerRequest;
import com.credlink.customer.dto.CustomerResponse;
import com.credlink.common.ApiResponse;
import com.credlink.security.CurrentMerchant;
import com.credlink.transaction.TransactionService;
import com.credlink.transaction.dto.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final TransactionService transactionService;
    private final CurrentMerchant currentMerchant;

    public CustomerController(CustomerService customerService, TransactionService transactionService, CurrentMerchant currentMerchant) {
        this.customerService = customerService;
        this.transactionService = transactionService;
        this.currentMerchant = currentMerchant;
    }

    @PostMapping
    public ApiResponse<CustomerResponse> create(@Valid @RequestBody CustomerRequest req) {
        return ApiResponse.ok(customerService.create(currentMerchant.id(), req));
    }

    @GetMapping
    public ApiResponse<List<CustomerResponse>> list(@RequestParam(required = false) String search) {
        return ApiResponse.ok(customerService.list(currentMerchant.id(), search));
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(customerService.get(currentMerchant.id(), id));
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerResponse> update(@PathVariable Long id, @Valid @RequestBody CustomerRequest req) {
        return ApiResponse.ok(customerService.update(currentMerchant.id(), id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, String>> delete(@PathVariable Long id) {
        customerService.delete(currentMerchant.id(), id);
        return ApiResponse.ok(Map.of("message", "Customer deleted"));
    }

    @GetMapping("/{id}/balance")
    public ApiResponse<Map<String, BigDecimal>> balance(@PathVariable Long id) {
        CustomerResponse c = customerService.get(currentMerchant.id(), id);
        return ApiResponse.ok(Map.of("balance", c.getCurrentBalance()));
    }

    @GetMapping("/{id}/transactions")
    public ApiResponse<List<TransactionResponse>> transactions(@PathVariable Long id) {
        return ApiResponse.ok(transactionService.historyForCustomer(currentMerchant.id(), id));
    }
}
