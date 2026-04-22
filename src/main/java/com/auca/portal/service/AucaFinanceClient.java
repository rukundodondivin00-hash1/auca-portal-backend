package com.auca.portal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AucaFinanceClient {

    @Value("${auca.finance.base-url}")
    private String aucaBaseUrl;

    @Value("${auca.finance.api-key:}")
    private String aucaApiKey;

    private final RestTemplate restTemplate;

    public AucaFinanceClient() {
        this.restTemplate = new RestTemplate();
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (aucaApiKey != null && !aucaApiKey.isEmpty()) {
            headers.set("Authorization", "Bearer " + aucaApiKey);
        }
        return headers;
    }

    // AUTH - Login to AUCA
    public Map<String, Object> login(String username, String password) {
        try {
            String url = aucaBaseUrl + "/api/v1/common/auth/signin";
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("username", username);
            requestBody.put("password", password);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, getHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }
            return null;
        } catch (Exception e) {
            System.err.println("Login error: " + e.getMessage());
            return null;
        }
    }

    // STUDENT - Get student data from AUCA
    public Map<String, Object> getStudentData(String studentId) {
        try {
            String url = aucaBaseUrl + "/api/v1/common/student?studentId=" + studentId;
            HttpEntity<String> entity = new HttpEntity<>(getHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }
            return null;
        } catch (Exception e) {
            System.err.println("Get student error: " + e.getMessage());
            return null;
        }
    }

    // FEE - Get fee structure
    public Map<String, Object> getFeeStructure(String departmentCode) {
        try {
            String url = aucaBaseUrl + "/api/v1/finance/fee-structure?departmentCode=" + departmentCode;
            HttpEntity<String> entity = new HttpEntity<>(getHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }
            return null;
        } catch (Exception e) {
            System.err.println("Get fee structure error: " + e.getMessage());
            return null;
        }
    }

    // BALANCE - Get student balance
    public Map<String, Object> getStudentBalance(String studentId) {
        try {
            String url = aucaBaseUrl + "/api/v1/finance/student-payments/" + studentId + "/balance";
            HttpEntity<String> entity = new HttpEntity<>(getHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }
            return null;
        } catch (Exception e) {
            System.err.println("Get balance error: " + e.getMessage());
            return null;
        }
    }

    // PAYMENTS - Get all payments for student
    public Map<String, Object> getStudentPayments(String studentId) {
        try {
            String url = aucaBaseUrl + "/api/v1/finance/student-payments/" + studentId;
            HttpEntity<String> entity = new HttpEntity<>(getHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }
            return null;
        } catch (Exception e) {
            System.err.println("Get payments error: " + e.getMessage());
            return null;
        }
    }

    // INITIATE - Initiate payment
    public Map<String, Object> initiatePayment(Map<String, Object> paymentRequest) {
        try {
            String url = aucaBaseUrl + "/api/v1/finance/student-payments/initiate";
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(paymentRequest, getHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }
            return null;
        } catch (Exception e) {
            System.err.println("Initiate payment error: " + e.getMessage());
            return null;
        }
    }

    // SAVE PAYMENT - Save payment to AUCA
    public boolean savePaymentToAuca(Map<String, Object> paymentData) {
        try {
            String url = aucaBaseUrl + "/api/v1/finance/student-payments";
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(paymentData, getHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            return response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED;
        } catch (Exception e) {
            System.err.println("Save payment error: " + e.getMessage());
            return false;
        }
    }

    // RECALCULATE - Recalculate balance
    public boolean recalculateStudentBalance(String studentId) {
        try {
            String url = aucaBaseUrl + "/api/v1/finance/student-payments/" + studentId + "/calculate-balance";
            HttpEntity<String> entity = new HttpEntity<>(getHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            System.err.println("Recalculate balance error: " + e.getMessage());
            return false;
        }
    }
}