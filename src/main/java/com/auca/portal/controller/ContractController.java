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
@Tag(
    name = "Contract Management",
    description = """
        APIs for student contract creation and management with AUCA Finance integration.
        This controller handles contract lifecycle operations including eligibility verification,
        contract generation, and retrieval of contract details with real-time synchronization
        from the AUCA Finance system.
        \n\n
        **Key Operations:**\n
        - Fetch student data and balance from AUCA\n
        - Create contracts after payment validation (minimum 50% required)\n
        - Retrieve contract details with payment progress tracking\n
        \n\n
        All responses follow a standard format with a `success` flag, data payload, and
        appropriate HTTP status codes. Error responses include descriptive error messages.
        """
)
public class ContractController {

    @Autowired
    private ContractService contractService;

    @Autowired
    private AucaFinanceClient aucaFinanceClient;

    @Operation(
        summary = "Retrieve Student Data from AUCA System",
        description = "Fetches comprehensive student information from the AUCA Finance system including department details, program information, fee structure, and current outstanding balance. This endpoint integrates with the external AUCA IMS API to retrieve real-time student data."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Student data successfully retrieved from AUCA system",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                    {
                      "success": true,
                      "student": {
                        "studentId": "STU001",
                        "name": "John Doe",
                        "email": "john.doe@auca.ac.rw",
                        "departmentCode": "CS",
                        "program": "Computer Science"
                      },
                      "feeStructure": {
                        "creditPrice": 500000,
                        "registrationFee": 50000,
                        "graduationFee": 100000
                      },
                      "balance": {
                        "balance": 250000
                      }
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - student ID format is incorrect",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                    {
                      "success": false,
                      "error": "Invalid student ID format"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Student not found in AUCA system",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                    {
                      "success": false,
                      "error": "Student not found in AUCA"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error or AUCA service unavailable",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                    {
                      "success": false,
                      "error": "Failed to connect to AUCA Finance service"
                    }
                    """)
            )
        )
    })
    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentFromAuca(
            @Parameter(
                description = "Unique student identifier (e.g., STU001, STU002)",
                required = true,
                example = "STU001",
                schema = @Schema(pattern = "^STU\\d{3}$", minLength = 6, maxLength = 7)
            )
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
        summary = "Retrieve Student Balance from AUCA System",
        description = "Fetches the current outstanding balance for a specific student from the AUCA Finance system. This endpoint provides real-time balance information needed for contract eligibility verification."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Balance successfully retrieved",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                    {
                      "success": true,
                      "balance": {
                        "balance": 250000,
                        "studentId": "STU001",
                        "currency": "RWF"
                      }
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - student ID format is incorrect",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                    {
                      "success": false,
                      "error": "Invalid student ID format"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Balance not found for the specified student",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                    {
                      "success": false,
                      "error": "Balance not found for student",
                      "studentId": "STU001"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error or AUCA service unavailable",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                    {
                      "success": false,
                      "error": "Failed to retrieve balance from AUCA Finance service"
                    }
                    """)
            )
        )
    })
    @GetMapping("/{studentId}/balance")
    public ResponseEntity<?> getBalance(
            @Parameter(
                description = "Unique student identifier (e.g., STU001, STU002)",
                required = true,
                example = "STU001",
                schema = @Schema(pattern = "^STU\\d{3}$", minLength = 6, maxLength = 7)
            )
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
        summary = "Create Student Contract",
        description = """
            Creates a new contract for a student after validating payment eligibility.
            The system verifies that the student has paid at least 50% of the total fees
            before contract creation. Contract information is stored in the local database
            and synchronized with AUCA Finance data.
            \n\n
            **Eligibility Criteria:**\n
            - Student must exist in AUCA Finance system\n
            - Student must have paid at least 50% of total fees\n
            - Student must have valid department code\n
            \n\n
            **Contract Status Rules:**\n
            - If outstanding balance is 0, status = \"COMPLETED\"\n
            - If outstanding balance > 0 and payment >= 50%, status = \"ELIGIBLE_FOR_CONTRACT\"
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Contract successfully created",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                    {
                      "success": true,
                      "contract": {
                        "contractId": 1,
                        "studentId": "STU001",
                        "studentName": "John Doe",
                        "totalAmount": 750000,
                        "paidAmount": 400000,
                        "remainingAmount": 350000,
                        "status": "ELIGIBLE_FOR_CONTRACT",
                        "paymentProgress": 53.33,
                        "startDate": "2026-04-26",
                        "endDate": "2027-06-30"
                      },
                      "student": {
                        "studentId": "STU001",
                        "name": "John Doe",
                        "email": "john.doe@auca.ac.rw",
                        "departmentCode": "CS"
                      },
                      "feeStructure": {
                        "creditPrice": 500000,
                        "registrationFee": 50000,
                        "graduationFee": 100000,
                        "totalFees": 750000
                      },
                      "balanceFromAuca": 350000,
                      "paidFromAuca": 400000
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Contract creation failed - payment too low or invalid data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                    {
                      "success": false,
                      "error": "Payment too low. Student must pay at least 50%.",
                      "paidAmount": 300000,
                      "totalAmount": 750000,
                      "remainingBalance": 450000,
                      "requiredAmount": 375000
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Student not found in AUCA database",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                    {
                      "success": false,
                      "error": "Student not found in AUCA database"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Invalid request payload - missing required fields",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                    {
                      "success": false,
                      "error": "Required field 'studentId' is missing"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during contract creation",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                    {
                      "success": false,
                      "error": "Database connection error or unexpected system failure"
                    }
                    """)
            )
        )
    })
    @PostMapping("/create")
    public ResponseEntity<?> createContract(
            @Parameter(
                description = "Contract creation request containing student ID and optional notification email",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                          "studentId": "STU001",
                          "notificationEmail": "student@email.com",
                          "startDate": "2026-04-26"
                        }
                        """)
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
        summary = "Retrieve Contract by Student ID",
        description = """
            Retrieves a student's contract details including payment progress, status, and related information.
            This endpoint returns comprehensive contract data along with the latest student information
            from AUCA Finance system, current balance, and payment history.
            \n\n
            **Response includes:**\n
            - Contract details (amounts, status, dates)\n
            - Student information from AUCA\n
            - Current balance from AUCA\n
            - Payment history from AUCA\n
            \n\n
            **Contract Status Values:**\n
            - `ELIGIBLE_FOR_CONTRACT`: Student has paid >= 50% but has remaining balance\n
            - `COMPLETED`: Student has fully paid all fees\n
            - `PENDING`: Contract not yet created (not in use)
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Contract successfully retrieved",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                    {
                      "success": true,
                      "contract": {
                        "contractId": 1,
                        "studentId": "STU001",
                        "studentName": "John Doe",
                        "totalAmount": 750000,
                        "paidAmount": 400000,
                        "remainingAmount": 350000,
                        "status": "ELIGIBLE_FOR_CONTRACT",
                        "paymentProgress": 53.33,
                        "startDate": "2026-04-26",
                        "endDate": "2027-06-30"
                      },
                      "studentFromAuca": {
                        "studentId": "STU001",
                        "name": "John Doe",
                        "email": "john.doe@auca.ac.rw",
                        "departmentCode": "CS",
                        "program": "Computer Science"
                      },
                      "balanceFromAuca": {
                        "balance": 350000,
                        "studentId": "STU001"
                      },
                      "paymentsFromAuca": [
                        {
                          "paymentId": "PAY001",
                          "amount": 200000,
                          "date": "2026-01-15",
                          "type": "TUITION"
                        },
                        {
                          "paymentId": "PAY002",
                          "amount": 200000,
                          "date": "2026-02-20",
                          "type": "TUITION"
                        }
                      ]
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No contract found for the specified student ID",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                    {
                      "success": false,
                      "error": "Contract not found"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid student ID format",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                    {
                      "success": false,
                      "error": "Invalid student ID format"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error or AUCA service unavailable",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                    {
                      "success": false,
                      "error": "Failed to retrieve contract data"
                    }
                    """)
            )
        )
    })
    @GetMapping("/{studentId}")
    public ResponseEntity<?> getContract(
            @Parameter(
                description = "Unique student identifier (e.g., STU001, STU002)",
                required = true,
                example = "STU001",
                schema = @Schema(pattern = "^STU\\d{3}$", minLength = 6, maxLength = 7)
            )
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
