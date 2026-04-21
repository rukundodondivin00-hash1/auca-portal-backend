package com.auca.portal.controller;

import com.auca.portal.dto.PaymentInitiateRequest;
import com.auca.portal.entity.Payment;
import com.auca.portal.repository.PaymentRepository;
import com.auca.portal.service.AucaFinanceClient;
import com.auca.portal.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    
    @Autowired
    private ContractService contractService;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private AucaFinanceClient aucaFinanceClient;
    
    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@RequestBody PaymentInitiateRequest request) {
        Map<String, Object> aucaPaymentRequest = new HashMap<>();
        aucaPaymentRequest.put("phoneNumber", request.getPhoneNumber());
        aucaPaymentRequest.put("amount", request.getAmount());
        aucaPaymentRequest.put("channelName", request.getChannelName());
        aucaPaymentRequest.put("payerEmail", request.getPayerEmail());
        aucaPaymentRequest.put("feeType", "TUITION_FEE");
        aucaPaymentRequest.put("payerCode", request.getPayerCode());
        aucaPaymentRequest.put("payerNames", request.getPayerNames());
        
        Map<String, Object> aucaResponse = aucaFinanceClient.initiatePayment(aucaPaymentRequest);
        
        if (aucaResponse != null && aucaResponse.get("urubutoTransactionId") != null) {
            String transactionId = (String) aucaResponse.get("urubutoTransactionId");
            
            contractService.createPayment(
                request.getPayerCode(),
                request.getInstallmentId(),
                request.getAmount(),
                request.getChannelName(),
                request.getPhoneNumber(),
                request.getPayerNames(),
                request.getPayerEmail(),
                request.getServiceCode(),
                transactionId
            );
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("transaction_id", transactionId);
            result.put("reference_id", aucaResponse.get("referenceId"));
            result.put("card_processing_url", aucaResponse.get("cardProcessingUrl"));
            result.put("message", "Payment initiated. Check your phone for USSD prompt.");
            
            return ResponseEntity.ok(result);
        }
        
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", "Payment initiation failed through AUCA");
        return ResponseEntity.badRequest().body(error);
    }
    
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<?> checkStatus(@PathVariable String transactionId) {
        Optional<Payment> paymentOpt = paymentRepository.findByTransactionId(transactionId);
        
        Map<String, Object> result = new HashMap<>();
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            result.put("status", payment.getStatus());
            result.put("slip_number", payment.getSlipNumber());
        } else {
            result.put("status", "NOT_FOUND");
        }
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/callback")
    public ResponseEntity<?> paymentCallback(@RequestBody Map<String, Object> callback) {
        String transactionId = (String) callback.get("transaction_id");
        String transactionStatus = (String) callback.get("transaction_status");
        String slipNumber = (String) callback.get("slip_number");
        
        Optional<Payment> paymentOpt = paymentRepository.findByTransactionId(transactionId);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(transactionStatus);
            payment.setSlipNumber(slipNumber);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            
            if ("VALID".equals(transactionStatus)) {
                contractService.updatePaymentStatus(transactionId, transactionStatus, slipNumber);
                
                Map<String, Object> aucaPaymentData = new HashMap<>();
                if (payment.getContract() != null) {
                    aucaPaymentData.put("studentId", payment.getContract().getStudentId());
                }
                aucaPaymentData.put("amount", payment.getAmount());
                aucaPaymentData.put("transactionId", transactionId);
                aucaPaymentData.put("status", transactionStatus);
                aucaPaymentData.put("slipNumber", slipNumber);
                
                aucaFinanceClient.savePaymentToAuca(aucaPaymentData);
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/notification")
    public ResponseEntity<?> paymentNotification(@RequestBody Map<String, Object> notification) {
        String transactionId = (String) notification.get("transaction_id");
        String transactionStatus = (String) notification.get("transaction_status");
        String slipNumber = (String) notification.get("slip_number");
        
        Optional<Payment> paymentOpt = paymentRepository.findByTransactionId(transactionId);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(transactionStatus);
            payment.setSlipNumber(slipNumber);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            
            if ("VALID".equals(transactionStatus)) {
                contractService.updatePaymentStatus(transactionId, transactionStatus, slipNumber);
                
                Map<String, Object> aucaPaymentData = new HashMap<>();
                if (payment.getContract() != null) {
                    aucaPaymentData.put("studentId", payment.getContract().getStudentId());
                }
                aucaPaymentData.put("amount", payment.getAmount());
                aucaPaymentData.put("transactionId", transactionId);
                aucaPaymentData.put("status", transactionStatus);
                aucaPaymentData.put("slipNumber", slipNumber);
                
                boolean saved = aucaFinanceClient.savePaymentToAuca(aucaPaymentData);
                if (saved && payment.getContract() != null) {
                    aucaFinanceClient.recalculateStudentBalance(payment.getContract().getStudentId());
                }
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        return ResponseEntity.ok(response);
    }
}