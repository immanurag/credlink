package com.credlink.nlp;

import com.credlink.nlp.dto.NlpExtractionResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LlmResponseParserTest {

    private final LlmResponseParser parser = new LlmResponseParser();

    @Test
    void parsesValidCompletionEnvelope() {
        String body = """
            {"choices":[{"message":{"content":"{\\"customerName\\":\\"Ramesh\\",\\"amount\\":200,\\"type\\":\\"CREDIT\\"}"}}]}
            """;
        NlpExtractionResult r = parser.parseCompletionEnvelope(body);
        assertTrue(r.isValid());
        assertEquals("Ramesh", r.getCustomerName());
        assertEquals(200.0, r.getAmount());
        assertEquals("UDHAAR", r.getType());
    }

    @Test
    void normalizesJamaTypeAliases() {
        NlpExtractionResult r = parser.parseExtractedJson("{\"customerName\":\"Suresh\",\"amount\":300,\"type\":\"DEBIT\"}");
        assertTrue(r.isValid());
        assertEquals("JAMA", r.getType());
    }

    @Test
    void rejectsMissingFields() {
        NlpExtractionResult r = parser.parseExtractedJson("{\"customerName\":\"Ramesh\"}");
        assertFalse(r.isValid());
    }

    @Test
    void rejectsNonPositiveAmount() {
        NlpExtractionResult r = parser.parseExtractedJson("{\"customerName\":\"Ramesh\",\"amount\":0,\"type\":\"UDHAAR\"}");
        assertFalse(r.isValid());
    }

    @Test
    void rejectsNoJsonInContent() {
        NlpExtractionResult r = parser.parseCompletionEnvelope("{\"choices\":[{\"message\":{\"content\":\"I could not parse this.\"}}]}");
        assertFalse(r.isValid());
    }
}
