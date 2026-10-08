package com.credlink.nlp;

import com.credlink.nlp.dto.NlpExtractionResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleBasedNlpServiceTest {

    private final RuleBasedNlpService service = new RuleBasedNlpService();

    @Test
    void extractsDigitAmountAndUdhaarType() {
        NlpExtractionResult r = service.extract("Ramesh se 200 rupaye credit kiya");
        assertTrue(r.isValid());
        assertEquals("Ramesh", r.getCustomerName());
        assertEquals(200.0, r.getAmount());
        assertEquals("UDHAAR", r.getType());
        assertNotNull(r.getEnglishDescription());
        assertFalse(r.getEnglishDescription().matches(".*[\u0900-\u097F].*"));
    }

    @Test
    void extractsJamaFromPaymentKeyword() {
        NlpExtractionResult r = service.extract("Suresh se 500 rupaye jama mila");
        assertTrue(r.isValid());
        assertEquals("Suresh", r.getCustomerName());
        assertEquals(500.0, r.getAmount());
        assertEquals("JAMA", r.getType());
        assertEquals("I received 500 rupees from Suresh.", r.getEnglishDescription());
    }

    @Test
    void extractsHindiWordNumber() {
        NlpExtractionResult r = service.extract("Ramesh ko do sau udhaar diya");
        assertTrue(r.isValid());
        assertEquals(200.0, r.getAmount());
        assertEquals("UDHAAR", r.getType());
    }

    @Test
    void extractsHindiScriptExample1_MohanLeneHain() {
        NlpExtractionResult r = service.extract("मोहन से पाँच सौ रुपये लेने हैं");
        assertTrue(r.isValid());
        assertEquals("Mohan", r.getCustomerName());
        assertEquals(500.0, r.getAmount());
        assertEquals("UDHAAR", r.getType());
        assertEquals("I have to collect 500 rupees from Mohan.", r.getEnglishDescription());
    }

    @Test
    void extractsHindiScriptExample2_RahulDeneHain() {
        NlpExtractionResult r = service.extract("राहुल को दो हजार रुपये देने हैं");
        assertTrue(r.isValid());
        assertEquals("Rahul", r.getCustomerName());
        assertEquals(2000.0, r.getAmount());
        assertEquals("JAMA", r.getType());
        assertEquals("I have to pay Rahul 2000 rupees.", r.getEnglishDescription());
    }

    @Test
    void extractsHindiScriptExample3_SonuLiye() {
        NlpExtractionResult r = service.extract("सोनू से 500 रुपये लिए");
        assertTrue(r.isValid());
        assertEquals("Sonu", r.getCustomerName());
        assertEquals(500.0, r.getAmount());
        assertEquals("JAMA", r.getType());
        assertEquals("I received 500 rupees from Sonu.", r.getEnglishDescription());
    }

    @Test
    void rejectsTranscriptWithNoAmount() {
        NlpExtractionResult r = service.extract("Ramesh se udhaar kiya");
        assertFalse(r.isValid());
        assertNotNull(r.getRejectionReason());
    }

    @Test
    void extractsStatementWithoutCustomerName() {
        NlpExtractionResult r = service.extract("500 rupaye diya");
        assertTrue(r.isValid());
        assertEquals("Customer", r.getCustomerName());
        assertEquals(500.0, r.getAmount());
        assertEquals("UDHAAR", r.getType());

        NlpExtractionResult r2 = service.extract("1000 received");
        assertTrue(r2.isValid());
        assertEquals("Customer", r2.getCustomerName());
        assertEquals(1000.0, r2.getAmount());
        assertEquals("JAMA", r2.getType());
    }

    @Test
    void rejectsEmptyTranscript() {
        NlpExtractionResult r = service.extract("");
        assertFalse(r.isValid());
    }

    @Test
    void testCase_AdityaJamaKarBaki() {
        NlpExtractionResult r = service.extract("आदित्य ने पाँच सौ रुपये जमा कर आठ सौ रुपये बकाया");
        assertTrue(r.isValid());
        assertEquals("Aditya", r.getCustomerName());
        assertEquals(500.0, r.getAmount());
        assertEquals("JAMA", r.getType());
        assertEquals(800.0, r.getOutstandingAmount());
        assertFalse(r.getEnglishDescription().contains("1300"));
    }

    @Test
    void extractsVishalExactPromptTestCase() {
        NlpExtractionResult r = service.extract("विशाल ने पाँच सौ रुपये जमा किए, एक हजार रुपये बकाया हैं।");
        assertTrue(r.isValid());
        assertEquals("Vishal", r.getCustomerName());
        assertEquals(500.0, r.getAmount());
        assertEquals("JAMA", r.getType());
        assertEquals(1000.0, r.getOutstandingAmount());
        assertFalse(r.getEnglishDescription().contains("1500"));
    }

    @Test
    void testCase1_RameshJama() {
        NlpExtractionResult r = service.extract("Ramesh ne 500 rupaye jama kiye.");
        assertTrue(r.isValid());
        assertEquals("Ramesh", r.getCustomerName());
        assertEquals(500.0, r.getAmount());
        assertEquals("JAMA", r.getType());
        assertNull(r.getOutstandingAmount());
    }

    @Test
    void testCase2_RameshBakiOnly() {
        NlpExtractionResult r = service.extract("Ramesh ke 1000 rupaye baki hain.");
        assertTrue(r.isValid());
        assertEquals("Ramesh", r.getCustomerName());
        assertEquals(1000.0, r.getAmount());
        assertEquals("UDHAAR", r.getType());
        assertEquals(1000.0, r.getOutstandingAmount());
    }

    @Test
    void testCase3_RameshJamaAndBaki() {
        NlpExtractionResult r = service.extract("Ramesh ne 500 rupaye jama kiye aur 1000 rupaye baki hain.");
        assertTrue(r.isValid());
        assertEquals("Ramesh", r.getCustomerName());
        assertEquals(500.0, r.getAmount());
        assertEquals("JAMA", r.getType());
        assertEquals(1000.0, r.getOutstandingAmount());
    }

    @Test
    void testCase4_SureshUdhaar() {
        NlpExtractionResult r = service.extract("Suresh ko 700 rupaye udhaar diye.");
        assertTrue(r.isValid());
        assertEquals("Suresh", r.getCustomerName());
        assertEquals(700.0, r.getAmount());
        assertEquals("UDHAAR", r.getType());
        assertNull(r.getOutstandingAmount());
    }

    @Test
    void testCase5_MohanJamaAndAbhiBaki() {
        NlpExtractionResult r = service.extract("Mohan ne 1000 rupaye jama kiye, 500 rupaye abhi baki hain.");
        assertTrue(r.isValid());
        assertEquals("Mohan", r.getCustomerName());
        assertEquals(1000.0, r.getAmount());
        assertEquals("JAMA", r.getType());
        assertEquals(500.0, r.getOutstandingAmount());
    }

    @Test
    void testCase6_Aditya800Jama200Baki() {
        NlpExtractionResult r = service.extract("Aditya ne 800 rupaye jama kiye aur 200 rupaye baki hain.");
        assertTrue(r.isValid());
        assertEquals("Aditya", r.getCustomerName());
        assertEquals(800.0, r.getAmount());
        assertEquals("JAMA", r.getType());
        assertEquals(200.0, r.getOutstandingAmount());
    }
}
