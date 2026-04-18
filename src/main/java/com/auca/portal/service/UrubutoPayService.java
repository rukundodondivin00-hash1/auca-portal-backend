package com.auca.portal.service;

import com.auca.portal.dto.PaymentInitiateRequest;
import com.auca.portal.dto.PaymentInitiateResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class UrubutoPayService {
    
    @Value("${urubutopay.base-url}")
    private String baseUrl;
    
    @Value("${urubutopay.api-key}")
    private String apiKey;
    
    @Value("${urubutopay.merchant-code}")
    private String merchantCode;
    
    private final RestTemplate restTemplate;
    
    public UrubutoPayService() {
        this.restTemplate = new RestTemplate();
    }
    
    public boolean validateWallet(String phoneNumber, String channel) {
        String url = baseUrl + "/api/v2/payment/account-holder/validate";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, String> request = new HashMap<>();
        request.put("payer_phone_number", phoneNumber);
        request.put("payment_channel_name", channel);
        
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object valid = response.getBody().get("valid");
                return valid != null && Boolean.TRUE.equals(valid);
            }
        } catch (Exception e) {
            System.err.println("Wallet validation error: " + e.getMessage());
        }
        return false;
    }
    
    public PaymentInitiateResponse initiatePayment(PaymentInitiateRequest request) {
        String url = baseUrl + "/api/v2/payment/initiate";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> body = new HashMap<>();
        body.put("merchant_code", merchantCode);
        body.put("payer_code", request.getPayerCode());
        body.put("service_code", request.getServiceCode());
        body.put("amount", request.getAmount().toString());
        body.put("channel_name", request.getChannelName());
        body.put("phone_number", request.getPhoneNumber());
        body.put("payer_names", request.getPayerNames());
        body.put("payer_email", request.getPayerEmail());
        
        String transactionId = "TXN-" + System.currentTimeMillis();
        body.put("transaction_id", transactionId);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                PaymentInitiateResponse result = new PaymentInitiateResponse();
                Map<String, Object> bodyResponse = response.getBody();
                
                result.setResponseCode(String.valueOf(bodyResponse.get("response_code")));
                result.setResponseMessage((String) bodyResponse.get("response_message"));
                result.setTransactionId(transactionId);
                result.setCheckoutUrl((String) bodyResponse.get("card_processing_url"));
                result.setMessage((String) bodyResponse.get("message"));
                
                return result;
            }
        } catch (Exception e) {
            System.err.println("Payment initiation error: " + e.getMessage());
        }
        
        return null;
    }
    
    public String checkTransactionStatus(String transactionId) {
        String url = baseUrl + "/api/v2/payment/transaction/status";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, String> request = new HashMap<>();
        request.put("merchant_code", merchantCode);
        request.put("transaction_id", transactionId);
        
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("transaction_status");
            }
        } catch (Exception e) {
            System.err.println("Transaction status check error: " + e.getMessage());
        }
        return null;
    }
    
    public Double getWalletBalance() {
        String url = baseUrl + "/api/v2/wallet/balance";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object balance = response.getBody().get("available_balance");
                if (balance != null) {
                    return Double.parseDouble(balance.toString());
                }
            }
        } catch (Exception e) {
            System.err.println("Wallet balance error: " + e.getMessage());
        }
        return 0.0;
    }
}