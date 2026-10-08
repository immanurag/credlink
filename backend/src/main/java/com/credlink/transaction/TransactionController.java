package com.credlink.transaction;

import com.credlink.common.ApiResponse;
import com.credlink.security.CurrentMerchant;
import com.credlink.transaction.dto.TransactionPreviewRequest;
import com.credlink.transaction.dto.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final CurrentMerchant currentMerchant;

    public TransactionController(TransactionService transactionService, CurrentMerchant currentMerchant) {
        this.transactionService = transactionService;
        this.currentMerchant = currentMerchant;
    }

    @PostMapping
    public ApiResponse<TransactionResponse> create(@Valid @RequestBody TransactionPreviewRequest req) {
        // POST /transactions behaves as "preview" - it stays PENDING_REVIEW until /confirm is called.
        return ApiResponse.ok(transactionService.preview(currentMerchant.id(), req));
    }

    @PostMapping("/preview")
    public ApiResponse<TransactionResponse> preview(@Valid @RequestBody TransactionPreviewRequest req) {
        return ApiResponse.ok(transactionService.preview(currentMerchant.id(), req));
    }

    @GetMapping("/{id}")
    public ApiResponse<TransactionResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(transactionService.get(currentMerchant.id(), id));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<TransactionResponse> confirm(@PathVariable Long id,
                                                      @RequestBody(required = false) TransactionPreviewRequest edits) {
        return ApiResponse.ok(transactionService.confirm(currentMerchant.id(), id, edits));
    }

    @PostMapping("/confirm")
    public ApiResponse<TransactionResponse> confirmByBody(@Valid @RequestBody TransactionConfirmByIdRequest req) {
        return ApiResponse.ok(transactionService.confirm(currentMerchant.id(), req.getTransactionId(), req.getEdits()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, String>> discard(@PathVariable Long id) {
        transactionService.discard(currentMerchant.id(), id);
        return ApiResponse.ok(Map.of("message", "Transaction discarded"));
    }

    public static class TransactionConfirmByIdRequest {
        private Long transactionId;
        private TransactionPreviewRequest edits;
        public Long getTransactionId() { return transactionId; }
        public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
        public TransactionPreviewRequest getEdits() { return edits; }
        public void setEdits(TransactionPreviewRequest edits) { this.edits = edits; }
    }
}
