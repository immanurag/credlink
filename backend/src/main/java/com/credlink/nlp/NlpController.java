package com.credlink.nlp;

import com.credlink.common.ApiResponse;
import com.credlink.common.DevanagariTransliterator;
import com.credlink.common.exception.ApiException;
import com.credlink.customer.CustomerService;
import com.credlink.customer.dto.CustomerResponse;
import com.credlink.nlp.dto.NlpExtractionRequest;
import com.credlink.nlp.dto.NlpExtractionResult;
import com.credlink.security.CurrentMerchant;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/nlp")
public class NlpController {

    private static final Logger log = LoggerFactory.getLogger(NlpController.class);

    private final NlpServiceRouter router;
    private final CustomerService customerService;
    private final CurrentMerchant currentMerchant;

    public NlpController(NlpServiceRouter router, CustomerService customerService, CurrentMerchant currentMerchant) {
        this.router = router;
        this.customerService = customerService;
        this.currentMerchant = currentMerchant;
    }

    @PostMapping("/extract")
    public ApiResponse<NlpExtractionResult> extract(@Valid @RequestBody NlpExtractionRequest req) {
        NlpExtractionResult result = router.extract(req.getTranscript());
        if (!result.isValid()) {
            throw ApiException.badRequest("NLP_EXTRACTION_FAILED", result.getRejectionReason());
        }

        if ("OUTSTANDING_BALANCE_QUERY".equalsIgnoreCase(result.getIntent())) {
            Long merchantId = currentMerchant.id();
            String searchQuery = result.getCustomerName();
            List<CustomerResponse> matches = customerService.list(merchantId, searchQuery);

            Double dbBalance = null;
            boolean isDevanagari = DevanagariTransliterator.containsDevanagari(req.getTranscript());
            String rawDevanagari = isDevanagari ? RuleBasedNlpService.extractRawDevanagariName(req.getTranscript()) : null;

            if (!matches.isEmpty()) {
                CustomerResponse c = matches.get(0);
                result.setCustomerName(c.getName());
                result.setCustomerFound(true);
                BigDecimal bal = c.getCurrentBalance();
                dbBalance = bal.doubleValue();
                result.setOutstandingAmount(dbBalance);

                String displayName = (isDevanagari && rawDevanagari != null && !rawDevanagari.isBlank()) 
                        ? rawDevanagari 
                        : c.getName();

                String lowerTranscript = req.getTranscript().toLowerCase();
                boolean isUdharQuery = lowerTranscript.contains("udhar") || lowerTranscript.contains("udhaar") || req.getTranscript().contains("उधार");

                if (bal.compareTo(BigDecimal.ZERO) == 0) {
                    if (isUdharQuery) {
                        result.setResponseMessage(isDevanagari
                                ? displayName + " का कोई उधार नहीं है। उधार ₹0 है।"
                                : displayName + " ka koi udhar nahi hai. Udhar ₹0 hai.");
                    } else {
                        result.setResponseMessage(isDevanagari
                                ? displayName + " का कोई बकाया नहीं है। बकाया ₹0 है।"
                                : displayName + " ka koi baki nahi hai. Baki ₹0 hai.");
                    }
                } else if (bal.compareTo(BigDecimal.ZERO) < 0) {
                    String formattedBal = bal.abs().stripTrailingZeros().toPlainString();
                    if (isUdharQuery) {
                        result.setResponseMessage(isDevanagari
                                ? displayName + " का कोई उधार नहीं है। एडवांस ₹" + formattedBal + " जमा है।"
                                : displayName + " ka koi udhar nahi hai. Advance ₹" + formattedBal + " jama hai.");
                    } else {
                        result.setResponseMessage(isDevanagari
                                ? displayName + " का कोई बकाया नहीं है। एडवांस ₹" + formattedBal + " जमा है।"
                                : displayName + " ka koi baki nahi hai. Advance ₹" + formattedBal + " jama hai.");
                    }
                } else {
                    String formattedBal = bal.stripTrailingZeros().toPlainString();
                    if (isUdharQuery) {
                        result.setResponseMessage(isDevanagari
                                ? displayName + " का उधार ₹" + formattedBal + " है।"
                                : displayName + " ka udhar ₹" + formattedBal + " hai.");
                    } else {
                        result.setResponseMessage(isDevanagari
                                ? displayName + " का बकाया ₹" + formattedBal + " है।"
                                : displayName + " ka baki ₹" + formattedBal + " hai.");
                    }
                }
            } else {
                result.setCustomerFound(false);
                result.setOutstandingAmount(null);
                String displayName = (isDevanagari && rawDevanagari != null && !rawDevanagari.isBlank()) 
                        ? rawDevanagari 
                        : searchQuery;
                result.setResponseMessage(isDevanagari
                        ? displayName + " नाम का ग्राहक नहीं मिला।"
                        : "Customer '" + displayName + "' not found.");
            }

            log.info("--- VOICE NLP BALANCE QUERY DEBUG ---");
            log.info("RAW TRANSCRIPT:\n{}", req.getTranscript());
            log.info("NORMALIZED:\n{}", HindiNumberParser.normalizeWordNumbersToDigits(req.getTranscript()).toLowerCase());
            log.info("DETECTED CUSTOMER:\n{}", result.getCustomerName());
            log.info("DETECTED INTENT:\n{}", result.getIntent());
            log.info("DATABASE BALANCE:\n{}", dbBalance != null ? "₹" + dbBalance : "NOT FOUND");
            log.info("FINAL RESPONSE:\n{}", result.getResponseMessage());
        } else if ("STATEMENT_QUERY".equalsIgnoreCase(result.getIntent())) {
            Long merchantId = currentMerchant.id();
            String searchQuery = result.getCustomerName();
            List<CustomerResponse> matches = customerService.list(merchantId, searchQuery);
            if (!matches.isEmpty()) {
                CustomerResponse c = matches.get(0);
                result.setCustomerName(c.getName());
                result.setCustomerFound(true);
                result.setResponseMessage("Opening account statement for " + c.getName() + ".");
            } else {
                result.setCustomerFound(false);
                result.setResponseMessage("Customer '" + searchQuery + "' not found.");
            }
            log.info("--- VOICE NLP STATEMENT QUERY DEBUG ---");
            log.info("RAW TRANSCRIPT: {}", req.getTranscript());
            log.info("DETECTED CUSTOMER: {}", result.getCustomerName());
            log.info("FINAL RESPONSE: {}", result.getResponseMessage());
        } else {
            log.info("--- VOICE NLP EXTRACTION DEBUG ---");
            log.info("RAW TRANSCRIPT: {}", req.getTranscript());
            log.info("DETECTED INTENT: {}", result.getIntent());
            log.info("DETECTED CUSTOMER: {}", result.getCustomerName());
            log.info("AMOUNT: {}, TYPE: {}", result.getAmount(), result.getType());
        }

        return ApiResponse.ok(result);
    }
}
