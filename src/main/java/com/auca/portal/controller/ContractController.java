package com.auca.portal.controller;

import com.auca.portal.dto.ContractResponse;
import com.auca.portal.entity.StudentContract;
import com.auca.portal.service.AucaFinanceClient;
import com.auca.portal.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/contract")
public class ContractController {
    
    @Autowired
    private ContractService contractService;
    
    @Autowired
    private AucaFinanceClient aucaFinanceClient;
    
    // GET STUDENT DATA FROM AUCA
    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentFromAuca(@PathVariable String studentId) {
        Map<String, Object> studentData = aucaFinanceClient.getStudentData(studentId);
        
        if (studentData == null) {
            return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "error", "Student not found in AUCA"
            ));
        }
        
        // Get fee structure
        String departmentCode = (String) studentData.get("departmentCode");
        Map<String, Object> feeStructure = aucaFinanceClient.getFeeStructure(departmentCode);
        
        // Get balance
        Map<String, Object> balance = aucaFinanceClient.getStudentBalance(studentId);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "student", studentData,
            "feeStructure", feeStructure != null ? feeStructure : new HashMap<>(),
            "balance", balance != null ? balance : Map.of("balance", 0)
        ));
    }
    
    // GET BALANCE FROM AUCA
    @GetMapping("/{studentId}/balance")
    public ResponseEntity<?> getBalance(@PathVariable String studentId) {
        Map<String, Object> balance = aucaFinanceClient.getStudentBalance(studentId);
        
        if (balance == null) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "balance", Map.of("studentId", studentId, "balance", 0)
            ));
        }
        
        return ResponseEntity.ok(Map.of("success", true, "balance", balance));
    }
    
    // CREATE CONTRACT - Checks AUCA balance first
    @PostMapping("/create")
    public ResponseEntity<?> createContract(@RequestBody Map<String, Object> request) {
        String studentId = (String) request.get("studentId");
        String notificationEmail = (String) request.get("notificationEmail");
        LocalDate startDate = request.get("startDate") != null 
            ? LocalDate.parse((String) request.get("startDate")) 
            : LocalDate.now();
        
        // Get student from AUCA
        Map<String, Object> studentData = aucaFinanceClient.getStudentData(studentId);
        if (studentData == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Student not found in AUCA database"
            ));
        }
        
        // GET BALANCE FROM AUCA - KEY STEP!
        Map<String, Object> balanceData = aucaFinanceClient.getStudentBalance(studentId);
        
        BigDecimal balance = BigDecimal.ZERO;
        if (balanceData != null && balanceData.get("balance") != null) {
            balance = new BigDecimal(balanceData.get("balance").toString());
        }
        
        String departmentCode = (String) studentData.get("departmentCode");
        
        // Get fee structure from AUCA
        Map<String, Object> feeStructure = aucaFinanceClient.getFeeStructure(departmentCode);
        
        // Calculate total fees
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (feeStructure != null) {
            try {
                if (feeStructure.get("creditPrice") != null) {
                    totalAmount = totalAmount.add(new BigDecimal(feeStructure.get("creditPrice").toString()));
                }
                if (feeStructure.get("registrationFee") != null) {
                    totalAmount = totalAmount.add(new BigDecimal(feeStructure.get("registrationFee").toString()));
                }
                if (feeStructure.get("graduationFee") != null) {
                    totalAmount = totalAmount.add(new BigDecimal(feeStructure.get("graduationFee").toString()));
                }
            } catch (Exception e) {
                totalAmount = new BigDecimal("500000");
            }
        }
        
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            totalAmount = new BigDecimal("500000");
        }
        
        // Calculate how much student has paid
        BigDecimal paidAmount = totalAmount.subtract(balance);
        BigDecimal halfAmount = totalAmount.multiply(BigDecimal.valueOf(0.5));
        
        // Check eligibility based on actual payment from AUCA
        if (paidAmount.compareTo(halfAmount) < 0) {
            // PAID LESS THAN 50% - NOT ELIGIBLE
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Payment too low. You must pay at least 50% to be eligible for contract.",
                "paidAmount", paidAmount,
                "totalAmount", totalAmount,
                "balance", balance,
                "requiredAmount", halfAmount
            ));
        }
        
        // ELIGIBLE - Create contract
        if (notificationEmail == null || notificationEmail.isEmpty()) {
            notificationEmail = (String) studentData.get("email");
        }
        
        StudentContract contract = contractService.createContract(
            studentId, 
            (String) studentData.get("name"), 
            (String) studentData.get("email"), 
            notificationEmail, 
            totalAmount, 
            startDate
        );
        
        // Update with actual paid amount from AUCA
        contract.setPaidAmount(paidAmount);
        contract.setRemainingAmount(balance);
        contract.setContractEligible(true);
        
        if (balance.compareTo(BigDecimal.ZERO) == 0) {
            contract.setStatus("COMPLETED");
        } else {
            contract.setStatus("ELIGIBLE_FOR_CONTRACT");
        }
        
        ContractResponse response = mapToContractResponse(contract);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "contract", response,
            "student", studentData,
            "feeStructure", feeStructure != null ? feeStructure : new HashMap<>(),
            "balanceFromAuca", balance,
            "paidFromAuca", paidAmount
        ));
    }
    
    // GET CONTRACT
    @GetMapping("/{studentId}")
    public ResponseEntity<?> getContract(@PathVariable String studentId) {
        return contractService.getContractByStudentId(studentId)
            .map(contract -> {
                ContractResponse response = mapToContractResponse(contract);
                
                // Get real data from AUCA
                Map<String, Object> studentData = aucaFinanceClient.getStudentData(studentId);
                Map<String, Object> balance = aucaFinanceClient.getStudentBalance(studentId);
                Map<String, Object> aucaPayments = aucaFinanceClient.getStudentPayments(studentId);
                
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "contract", response,
                    "studentFromAuca", studentData != null ? studentData : new HashMap<>(),
                    "balanceFromAuca", balance != null ? balance : Map.of("balance", 0),
                    "paymentsFromAuca", aucaPayments != null ? aucaPayments : new HashMap<>()
                ));
            })
            .orElse(ResponseEntity.status(404).body(Map.of(
                "success", false,
                "error", "Contract not found"
            )));
    }
    
    // GET INSTALLMENTS
    @GetMapping("/{studentId}/installments")
    public ResponseEntity<?> getInstallments(@PathVariable String studentId) {
        var installments = contractService.getInstallmentsByStudentId(studentId);
        
        List<Map<String, Object>> installmentList = installments.stream()
            .map(inst -> {
                Map<String, Object> map = new HashMap<>();
                map.put("installmentId", inst.getInstallmentId());
                map.put("installmentNumber", inst.getInstallmentNumber());
                map.put("amount", inst.getAmount());
                map.put("paidAmount", inst.getPaidAmount());
                map.put("dueDate", inst.getDueDate());
                map.put("paidDate", inst.getPaidDate());
                map.put("status", inst.getStatus());
                map.put("slipNumber", inst.getSlipNumber());
                map.put("isOverdue", inst.getIsOverdue());
                return map;
            })
            .toList();
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "installments", installmentList
        ));
    }
    
    // GET PAYMENTS
    @GetMapping("/{studentId}/payments")
    public ResponseEntity<?> getPayments(@PathVariable String studentId) {
        var payments = contractService.getPaymentsByStudentId(studentId);
        
        List<Map<String, Object>> paymentList = payments.stream()
            .map(pay -> {
                Map<String, Object> map = new HashMap<>();
                map.put("paymentId", pay.getPaymentId());
                map.put("amount", pay.getAmount());
                map.put("status", pay.getStatus());
                map.put("channel", pay.getChannel());
                map.put("paymentDate", pay.getPaymentDate());
                map.put("slipNumber", pay.getSlipNumber());
                map.put("transactionId", pay.getTransactionId());
                return map;
            })
            .toList();
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "payments", paymentList
        ));
    }
    
    // UPDATE PAYMENT STATUS
    @PostMapping("/payment/update")
    public ResponseEntity<?> updatePaymentStatus(@RequestBody Map<String, String> request) {
        String transactionId = request.get("transactionId");
        String status = request.get("status");
        String slipNumber = request.get("slipNumber");
        
        contractService.updatePaymentStatus(transactionId, status, slipNumber);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Payment status updated"
        ));
    }
    
    private ContractResponse mapToContractResponse(StudentContract contract) {
        ContractResponse response = new ContractResponse();
        response.setContractId(contract.getContractId());
        response.setStudentId(contract.getStudentId());
        response.setStudentName(contract.getStudentName());
        response.setTotalAmount(contract.getTotalAmount());
        response.setPaidAmount(contract.getPaidAmount());
        response.setRemainingAmount(contract.getRemainingAmount());
        response.setStatus(contract.getStatus());
        response.setStartDate(contract.getStartDate());
        response.setEndDate(contract.getEndDate());
        
        double progress = 0;
        if (contract.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            progress = contract.getPaidAmount()
                .divide(contract.getTotalAmount(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
        }
        response.setPaymentProgress(progress);
        
        return response;
    }
}