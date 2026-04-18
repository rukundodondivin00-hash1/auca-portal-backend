package com.auca.portal.controller;

import com.auca.portal.dto.PaymentInitiateRequest;
import com.auca.portal.dto.PaymentInitiateResponse;
import com.auca.portal.entity.Payment;
import com.auca.portal.entity.StudentContract;
import com.auca.portal.repository.PaymentRepository;
import com.auca.portal.service.ContractService;
import com.auca.portal.service.UrubutoPayService;
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
    private UrubutoPayService urubutoPayService;
    
    @Autowired
    private ContractService contractService;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@RequestBody PaymentInitiateRequest request) {
        // First validate wallet
        boolean validPhone = urubutoPayService.validateWallet(
            request.getPhoneNumber(), request.getChannelName());
        
        if (!validPhone) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Invalid phone number or account not registered");
            return ResponseEntity.badRequest().body(error);
        }
        
        // Initiate payment with UrubutoPay
        PaymentInitiateResponse response = urubutoPayService.initiatePayment(request);
        
        if (response != null && "200".equals(response.getResponseCode())) {
            // Create payment record in our system
            String transactionId = response.getTransactionId();
            
            contractService.createPayment(
                request.getPayerCode(),
                request.getInstallmentId() != null ? request.getInstallmentId() : null,
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
            result.put("message", response.getMessage() != null ? response.getMessage() : "Payment initiated. Check your phone for USSD prompt.");
            
            return ResponseEntity.ok(result);
        }
        
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", "Payment initiation failed");
        return ResponseEntity.badRequest().body(error);
    }
    
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<?> checkStatus(@PathVariable String transactionId) {
        String status = urubutoPayService.checkTransactionStatus(transactionId);
        
        Optional<Payment> paymentOpt = paymentRepository.findByTransactionId(transactionId);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(status);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", status != null ? status : "UNKNOWN");
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/callback")
    public ResponseEntity<?> paymentCallback(@RequestBody Map<String, Object> callback) {
        String transactionId = (String) callback.get("transaction_id");
        String transactionStatus = (String) callback.get("transaction_status");
        
        Optional<Payment> paymentOpt = paymentRepository.findByTransactionId(transactionId);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(transactionStatus);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            
            // Update contract and installment
            contractService.updatePaymentStatus(transactionId, transactionStatus, null);
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
                System.out.println("Payment successful for transaction: " + transactionId + " Amount: " + payment.getAmount());
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        return ResponseEntity.ok(response);
    }
}