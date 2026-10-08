package com.credlink.customer;

import com.credlink.customer.dto.CustomerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerVoiceMatchingTest {

    private CustomerRepository repository;
    private CustomerService service;

    @BeforeEach
    void setup() {
        repository = mock(CustomerRepository.class);
        service = new CustomerService(repository);

        Customer c1 = new Customer();
        c1.setId(1L);
        c1.setMerchantId(10L);
        c1.setName("Sanchit");
        c1.setPhone("+919810244321");

        Customer c2 = new Customer();
        c2.setId(2L);
        c2.setMerchantId(10L);
        c2.setName("Rahul");
        c2.setPhone("+919876543210");

        Customer c3 = new Customer();
        c3.setId(3L);
        c3.setMerchantId(10L);
        c3.setName("Amit");
        c3.setPhone("+919123456789");

        when(repository.findByMerchantId(10L)).thenReturn(List.of(c1, c2, c3));
    }

    @Test
    void test1_SanchitKoFindKaro_Roman() {
        List<CustomerResponse> results = service.list(10L, "Sanchit ko find karo");
        assertFalse(results.isEmpty());
        assertEquals("Sanchit", results.get(0).getName());
    }

    @Test
    void test2_SanchitKoFindKaro_Devanagari() {
        List<CustomerResponse> results = service.list(10L, "संचित को find करो");
        assertFalse(results.isEmpty());
        assertEquals("Sanchit", results.get(0).getName());
    }

    @Test
    void test3_SanchitKaBalanceBatao_Roman() {
        List<CustomerResponse> results = service.list(10L, "Sanchit ka balance batao");
        assertFalse(results.isEmpty());
        assertEquals("Sanchit", results.get(0).getName());
    }

    @Test
    void test4_SanchitKaBalanceBatao_Devanagari() {
        List<CustomerResponse> results = service.list(10L, "संचित का balance बताओ");
        assertFalse(results.isEmpty());
        assertEquals("Sanchit", results.get(0).getName());
    }

    @Test
    void test5_RahulKoSearchKaro_Roman() {
        List<CustomerResponse> results = service.list(10L, "Rahul ko search karo");
        assertFalse(results.isEmpty());
        assertEquals("Rahul", results.get(0).getName());
    }

    @Test
    void test6_RahulKoKhojo_Devanagari() {
        List<CustomerResponse> results = service.list(10L, "राहुल को खोजो");
        assertFalse(results.isEmpty());
        assertEquals("Rahul", results.get(0).getName());
    }

    @Test
    void test7_AmitSeKitnePaiseLeneHain_Devanagari() {
        List<CustomerResponse> results = service.list(10L, "अमित से कितने पैसे लेने हैं");
        assertFalse(results.isEmpty());
        assertEquals("Amit", results.get(0).getName());
    }

    @Test
    void test8_NormalTextSearchPreserved() {
        List<CustomerResponse> results = service.list(10L, "Sanchit");
        assertFalse(results.isEmpty());
        assertEquals("Sanchit", results.get(0).getName());
    }

    @Test
    void test9_PhoneSearchPreserved() {
        List<CustomerResponse> results = service.list(10L, "9810244321");
        assertFalse(results.isEmpty());
        assertEquals("Sanchit", results.get(0).getName());
    }

    @Test
    void test10_MultipleMatchingCandidatesReturned() {
        Customer c1 = new Customer();
        c1.setId(1L);
        c1.setMerchantId(20L);
        c1.setName("Sanchit Kumar");

        Customer c2 = new Customer();
        c2.setId(2L);
        c2.setMerchantId(20L);
        c2.setName("Sanchit Sharma");

        when(repository.findByMerchantId(20L)).thenReturn(List.of(c1, c2));

        List<CustomerResponse> results = service.list(20L, "संचित को find करो");
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(c -> c.getName().equals("Sanchit Kumar")));
        assertTrue(results.stream().anyMatch(c -> c.getName().equals("Sanchit Sharma")));
    }
}
