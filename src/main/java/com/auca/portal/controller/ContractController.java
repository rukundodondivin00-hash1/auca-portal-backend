package com.auca.portal.controller;

import com.auca.portal.dto.ContractResponse;
import com.auca.portal.entity.StudentContract;
import com.auca.portal.service.AucaFinanceClient;
import com.auca.portal.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

        if (studentData == null || studentData.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "error", "Student not found in AUCA"
            ));
        }

        String departmentCode = (String) studentData.get("departmentCode");

        Map<String, Object> feeStructure = aucaFinanceClient.getFeeStructure(departmentCode);
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

        if (balance == null || balance.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "error", "Balance not found for student",
                    "studentId", studentId
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "balance", balance
        ));
    }

    // CREATE CONTRACT
    @PostMapping("/create")
    public ResponseEntity<?> createContract(@RequestBody Map<String, Object> request) {

        try {

            String studentId = (String) request.get("studentId");
            String notificationEmail = (String) request.get("notificationEmail");

            LocalDate startDate = request.get("startDate") != null
                    ? LocalDate.parse((String) request.get("startDate"))
                    : LocalDate.now();

            Map<String, Object> studentData = aucaFinanceClient.getStudentData(studentId);

            if (studentData == null || studentData.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Student not found in AUCA database"
                ));
            }

            Map<String, Object> balanceData = aucaFinanceClient.getStudentBalance(studentId);

            BigDecimal outstandingBalance = BigDecimal.ZERO;

            if (balanceData != null && balanceData.get("balance") != null) {
                outstandingBalance = new BigDecimal(balanceData.get("balance").toString());
            }

            String departmentCode = (String) studentData.get("departmentCode");
            Map<String, Object> feeStructure = aucaFinanceClient.getFeeStructure(departmentCode);

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

            BigDecimal paidAmount = totalAmount.subtract(outstandingBalance);
            BigDecimal halfAmount = totalAmount.multiply(BigDecimal.valueOf(0.5));

            if (paidAmount.compareTo(halfAmount) < 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Payment too low. Student must pay at least 50%.",
                        "paidAmount", paidAmount,
                        "totalAmount", totalAmount,
                        "remainingBalance", outstandingBalance,
                        "requiredAmount", halfAmount
                ));
            }

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

            contract.setPaidAmount(paidAmount);
            contract.setRemainingAmount(outstandingBalance);
            contract.setContractEligible(true);

            if (outstandingBalance.compareTo(BigDecimal.ZERO) == 0) {
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
                    "balanceFromAuca", outstandingBalance,
                    "paidFromAuca", paidAmount
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // GET CONTRACT
    @GetMapping("/{studentId}")
    public ResponseEntity<?> getContract(@PathVariable String studentId) {

        return contractService.getContractByStudentId(studentId)
                .map(contract -> {

                    ContractResponse response = mapToContractResponse(contract);

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
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "success", false,
                        "error", "Contract not found"
                )));
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
