package com.credlink.nlp;

import com.credlink.nlp.dto.NlpExtractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Single entry point for transcript -> structured transaction extraction.
 * Checks credlink.nlp.mode at CALL TIME (not via @Primary bean wiring) so the mock/live
 * choice is always evaluated fresh, and falls back to the rule-based extractor only when
 * the live provider genuinely fails (never silently prefers mock when live is configured).
 */
@Service
public class NlpServiceRouter {

    private static final Logger log = LoggerFactory.getLogger(NlpServiceRouter.class);

    private final RuleBasedNlpService ruleBasedNlpService;
    private final LiveLlmNlpService liveLlmNlpService;

    @Value("${credlink.nlp.mode}")
    private String mode; // mock | live

    public NlpServiceRouter(RuleBasedNlpService ruleBasedNlpService, LiveLlmNlpService liveLlmNlpService) {
        this.ruleBasedNlpService = ruleBasedNlpService;
        this.liveLlmNlpService = liveLlmNlpService;
    }

    public NlpExtractionResult extract(String transcript) {
        if (!"live".equalsIgnoreCase(mode)) {
            return ruleBasedNlpService.extract(transcript);
        }

        NlpExtractionResult liveResult = liveLlmNlpService.extract(transcript);
        if (liveResult.isValid()) {
            return liveResult;
        }

        log.warn("Live LLM extraction failed ({}), falling back to rule-based extractor.", liveResult.getRejectionReason());
        return ruleBasedNlpService.extract(transcript);
    }
}
