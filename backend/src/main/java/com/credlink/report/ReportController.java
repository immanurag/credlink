package com.credlink.report;

import com.credlink.common.ApiResponse;
import com.credlink.report.dto.StatementResponse;
import com.credlink.security.CurrentMerchant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final StatementService statementService;
    private final StatementExportService exportService;
    private final CurrentMerchant currentMerchant;

    public ReportController(StatementService statementService, StatementExportService exportService, CurrentMerchant currentMerchant) {
        this.statementService = statementService;
        this.exportService = exportService;
        this.currentMerchant = currentMerchant;
    }

    @GetMapping("/customers/{customerId}/statement")
    public ApiResponse<StatementResponse> statement(@PathVariable Long customerId,
                                                      @RequestParam(required = false) String from,
                                                      @RequestParam(required = false) String to) {
        return ApiResponse.ok(statementService.generate(currentMerchant.id(), customerId, parseInstant(from), parseInstant(to)));
    }

    @GetMapping("/customers/{customerId}/statement/export")
    public ResponseEntity<byte[]> export(@PathVariable Long customerId,
                                          @RequestParam(defaultValue = "pdf") String format,
                                          @RequestParam(required = false) String from,
                                          @RequestParam(required = false) String to) throws IOException {
        StatementResponse statement = statementService.generate(currentMerchant.id(), customerId, parseInstant(from), parseInstant(to));

        byte[] fileBytes;
        MediaType mediaType;
        String filename;
        if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
            fileBytes = exportService.toExcel(statement);
            mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            filename = "statement-" + customerId + ".xlsx";
        } else {
            fileBytes = exportService.toPdf(statement);
            mediaType = MediaType.APPLICATION_PDF;
            filename = "statement-" + customerId + ".pdf";
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(fileBytes);
    }

    private Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
