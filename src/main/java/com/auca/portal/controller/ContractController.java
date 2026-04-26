package com.auca.portal.controller;

import com.auca.portal.dto.ContractResponse;
import com.auca.portal.entity.StudentContract;
import com.auca.portal.service.AucaFinanceClient;
import com.auca.portal.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Contract Management", description = "APIs for contract creation and management")
public class ContractController {

    @Autowired
    private ContractService contractService;

    @Autowired
    private AucaFinanceClient aucaFinanceClient;

    @Operation(
        summary = "Get student data from AUCA system",
        description = "Retrieves comprehensive student information including department, program details, fee structure, and current balance"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Student data retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = "{success: true, student: {studentId: STU001}}")
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Student not found in AUCA system",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = "{success: false, error: Student not found}")
            )
        )
    })
    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentFromAuca(
            @Parameter(description = "Unique student identifier", required = true, example = "STU001")
            @PathVariable String studentId) {

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

    @Operation(
        summary = "Get student balance from AUCA",
        description = "Retrieves the current outstanding balance for a student"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Balance retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = "{success: true, balance: {balance: 250000}}")
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Balance not found for student",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = "{success: false, error: Balance not found}")
            )
        )
    })
    @GetMapping("/{studentId}/balance")
    public ResponseEntity<?> getBalance(
            @Parameter(description = "Unique student identifier", required = true, example = "STU001")
            @PathVariable String studentId) {

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

    @Operation(
        summary = "Create a new contract",
        description = "Creates a contract for a student after validating payment status. Student must have paid at least 50% of total fees."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Contract created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = "{success: true, contract: {}}")
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Payment too low or invalid student data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = "{success: false, error: Payment too low}")
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = "{success: false, error: Error message}")
            )
        )
    })
    @PostMapping("/create")
    public ResponseEntity<?> createContract(
            @Parameter(description = "Contract creation request payload with studentId", required = true)
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Contract creation request",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = "{studentId: STU001, notificationEmail: student@email.com, startDate: 2026-04-26}")
                )
            )
            @RequestBody Map<String, Object> request) {

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

    @Operation(
        summary = "Get contract by student ID",
        description = "Retrieves a student's contract details including payment progress, status, and related information"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Contract retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = "{success: true, contract: {}}")
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Contract not found for student",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = "{success: false, error: Contract not found}")
            )
        )
    })
    @GetMapping("/{studentId}")
    public ResponseEntity<?> getContract(
            @Parameter(description = "Unique student identifier", required = true, example = "STU001")
            @PathVariable String studentId) {

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
