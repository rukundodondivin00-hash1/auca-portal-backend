package com.auca.portal.controller;

import com.auca.portal.dto.ContractResponse;
import com.auca.portal.entity.StudentContract;
import com.auca.portal.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/contract")
public class ContractController {
    
    @Autowired
    private ContractService contractService;
    
    @GetMapping("/{studentId}")
    public ResponseEntity<?> getContract(@PathVariable String studentId) {
        return contractService.getContractByStudentId(studentId)
            .map(contract -> {
                ContractResponse response = mapToContractResponse(contract);
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/create")
    public ResponseEntity<?> createContract(@RequestBody Map<String, Object> request) {
        String studentId = (String) request.get("studentId");
        String studentName = (String) request.get("studentName");
        String studentEmail = (String) request.get("studentEmail");
        BigDecimal totalAmount = new BigDecimal(request.get("totalAmount").toString());
        LocalDate startDate = LocalDate.parse((String) request.get("startDate"));
        
        StudentContract contract = contractService.createContract(
            studentId, studentName, studentEmail, totalAmount, startDate
        );
        
        ContractResponse response = mapToContractResponse(contract);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{studentId}/installments")
    public ResponseEntity<?> getInstallments(@PathVariable String studentId) {
        List<ContractResponse.InstallmentDto> installments = contractService
            .getInstallmentsByStudentId(studentId)
            .stream()
            .map(inst -> {
                ContractResponse.InstallmentDto dto = new ContractResponse.InstallmentDto();
                dto.setInstallmentId(inst.getInstallmentId());
                dto.setInstallmentNumber(inst.getInstallmentNumber());
                dto.setAmount(inst.getAmount());
                dto.setPaidAmount(inst.getPaidAmount());
                dto.setDueDate(inst.getDueDate());
                dto.setPaidDate(inst.getPaidDate());
                dto.setStatus(inst.getStatus());
                dto.setSlipNumber(inst.getSlipNumber());
                return dto;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(installments);
    }
    
    @GetMapping("/{studentId}/payments")
    public ResponseEntity<?> getPayments(@PathVariable String studentId) {
        List<Map<String, Object>> payments = contractService.getPaymentsByStudentId(studentId)
            .stream()
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
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(payments);
    }
    
    @PostMapping("/payment/update")
    public ResponseEntity<?> updatePaymentStatus(@RequestBody Map<String, String> request) {
        String transactionId = request.get("transactionId");
        String status = request.get("status");
        String slipNumber = request.get("slipNumber");
        
        contractService.updatePaymentStatus(transactionId, status, slipNumber);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Payment status updated");
        
        return ResponseEntity.ok(response);
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
            progress = (contract.getPaidAmount().doubleValue() / contract.getTotalAmount().doubleValue()) * 100;
        }
        response.setPaymentProgress(progress);
        
        return response;
    }
}