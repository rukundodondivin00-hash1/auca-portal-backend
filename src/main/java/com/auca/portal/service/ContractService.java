package com.auca.portal.service;

import com.auca.portal.entity.Installment;
import com.auca.portal.entity.Payment;
import com.auca.portal.entity.StudentContract;
import com.auca.portal.repository.InstallmentRepository;
import com.auca.portal.repository.PaymentRepository;
import com.auca.portal.repository.StudentContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ContractService {
    
    @Autowired
    private StudentContractRepository contractRepository;
    
    @Autowired
    private InstallmentRepository installmentRepository;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    public StudentContract createContract(String studentId, String studentName, String studentEmail, 
            String notificationEmail, BigDecimal totalAmount, LocalDate startDate) {
        
        String contractId = "CON-" + studentId + "-" + System.currentTimeMillis();
        
        StudentContract contract = new StudentContract();
        contract.setContractId(contractId);
        contract.setStudentId(studentId);
        contract.setStudentName(studentName);
        contract.setStudentEmail(studentEmail);
        contract.setNotificationEmail(notificationEmail); // Set custom notification email
        contract.setTotalAmount(totalAmount);
        contract.setPaidAmount(BigDecimal.ZERO);
        contract.setRemainingAmount(totalAmount);
        contract.setStartDate(startDate);
        contract.setEndDate(startDate.plusMonths(3)); // 3-month payment period
        contract.setStatus("PENDING_INITIAL_PAYMENT");
        contract.setContractEligible(false); // Becomes true when 50% is paid
        contract.setPenaltyApplied(BigDecimal.ZERO);
        contract.setCreatedAt(LocalDateTime.now());
        contract.setUpdatedAt(LocalDateTime.now());
        
        contract = contractRepository.save(contract);
        
        createInstallments(contract, totalAmount);
        
        return contract;
    }
    
    private void createInstallments(StudentContract contract, BigDecimal totalAmount) {
        // New 3-month payment plan: 50% (Month 1), 25% (Month 2), 25% (Month 3)
        LocalDate startDate = contract.getStartDate();
        
        // Installment 1: 50% - Initial Payment (Month 1)
        Installment installment1 = new Installment();
        installment1.setInstallmentId("INST-" + contract.getStudentId() + "-1");
        installment1.setContract(contract);
        installment1.setInstallmentNumber(1);
        BigDecimal halfAmount = totalAmount.multiply(BigDecimal.valueOf(0.5));
        installment1.setAmount(halfAmount);
        installment1.setPaidAmount(BigDecimal.ZERO);
        installment1.setDueDate(startDate); // Immediate payment required
        installment1.setStatus("PENDING");
        installment1.setIsOverdue(false);
        installment1.setPenaltyAmount(BigDecimal.ZERO);
        installment1.setCreatedAt(LocalDateTime.now());
        installment1.setUpdatedAt(LocalDateTime.now());
        installmentRepository.save(installment1);
        
        // Installment 2: 25% - Second Month Payment
        Installment installment2 = new Installment();
        installment2.setInstallmentId("INST-" + contract.getStudentId() + "-2");
        installment2.setContract(contract);
        installment2.setInstallmentNumber(2);
        BigDecimal quarterAmount = totalAmount.multiply(BigDecimal.valueOf(0.25));
        installment2.setAmount(quarterAmount);
        installment2.setPaidAmount(BigDecimal.ZERO);
        installment2.setDueDate(startDate.plusMonths(1)); // Month 2
        installment2.setStatus("PENDING");
        installment2.setIsOverdue(false);
        installment2.setPenaltyAmount(BigDecimal.ZERO);
        installment2.setCreatedAt(LocalDateTime.now());
        installment2.setUpdatedAt(LocalDateTime.now());
        installmentRepository.save(installment2);
        
        // Installment 3: 25% - Third Month Payment
        Installment installment3 = new Installment();
        installment3.setInstallmentId("INST-" + contract.getStudentId() + "-3");
        installment3.setContract(contract);
        installment3.setInstallmentNumber(3);
        installment3.setAmount(quarterAmount);
        installment3.setPaidAmount(BigDecimal.ZERO);
        installment3.setDueDate(startDate.plusMonths(2)); // Month 3
        installment3.setStatus("PENDING");
        installment3.setIsOverdue(false);
        installment3.setPenaltyAmount(BigDecimal.ZERO);
        installment3.setCreatedAt(LocalDateTime.now());
        installment3.setUpdatedAt(LocalDateTime.now());
        installmentRepository.save(installment3);
    }
    
    public Optional<StudentContract> getContractByStudentId(String studentId) {
        return contractRepository.findByStudentId(studentId);
    }
    
    public List<Installment> getInstallmentsByStudentId(String studentId) {
        return installmentRepository.findByContract_StudentId(studentId);
    }
    
    public void updatePaymentStatus(String transactionId, String status, String slipNumber) {
        Optional<Payment> paymentOpt = paymentRepository.findByTransactionId(transactionId);
        
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(status);
            payment.setUpdatedAt(LocalDateTime.now());
            
            if ("VALID".equals(status) && slipNumber != null) {
                payment.setSlipNumber(slipNumber);
                payment.setPaymentDate(LocalDate.now());
                
                // Update installment
                if (payment.getInstallment() != null) {
                    Installment installment = payment.getInstallment();
                    installment.setStatus("PAID");
                    installment.setPaidDate(LocalDate.now());
                    installment.setSlipNumber(slipNumber);
                    installment.setTransactionId(transactionId);
                    installment.setPaymentChannel(payment.getChannel());
                    
                    // Check if payment is overdue and apply 5% penalty
                    if (LocalDate.now().isAfter(installment.getDueDate())) {
                        installment.setIsOverdue(true);
                        // 5% penalty on remaining 50% (only for installments 2 and 3)
                        if (installment.getInstallmentNumber() > 1) {
                            BigDecimal remainingBalance = payment.getContract().getTotalAmount()
                                .multiply(BigDecimal.valueOf(0.5)); // 50% remaining
                            BigDecimal penalty = remainingBalance.multiply(BigDecimal.valueOf(0.05));
                            installment.setPenaltyAmount(penalty);
                            
                            // Update contract penalty
                            payment.getContract().setPenaltyApplied(penalty);
                        }
                    }
                    
                    installment.setUpdatedAt(LocalDateTime.now());
                    installmentRepository.save(installment);
                    
                    // Update contract
                    updateContractPaidAmount(installment.getContract().getStudentId());
                    
                    // Send appropriate email notification
                    StudentContract updatedContract = installment.getContract();
                    if (Boolean.TRUE.equals(updatedContract.getContractEligible()) && 
                        updatedContract.getPaidAmount().compareTo(updatedContract.getTotalAmount()) < 0) {
                        // Just became eligible - send eligibility email
                    } else if (updatedContract.getStatus().equals("COMPLETED")) {
                        // Fully paid - send completion email
                    }
                }
            }
            
            paymentRepository.save(payment);
        }
    }
    
    private void updateContractPaidAmount(String studentId) {
        Optional<StudentContract> contractOpt = contractRepository.findByStudentId(studentId);
        
        if (contractOpt.isPresent()) {
            StudentContract contract = contractOpt.get();
            List<Installment> installments = installmentRepository.findByContract_StudentId(studentId);
            
            BigDecimal totalPaid = installments.stream()
                .filter(inst -> "PAID".equals(inst.getStatus()))
                .map(Installment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            contract.setPaidAmount(totalPaid);
            contract.setRemainingAmount(contract.getTotalAmount().subtract(totalPaid));
            contract.setUpdatedAt(LocalDateTime.now());
            
            BigDecimal halfAmount = contract.getTotalAmount().divide(BigDecimal.valueOf(2));
            
            // Check if 50% is paid - student becomes eligible for contract
            if (contract.getPaidAmount().compareTo(halfAmount) >= 0) {
                contract.setContractEligible(true);
                contract.setStatus("ELIGIBLE_FOR_CONTRACT");
            } else {
                contract.setContractEligible(false);
                contract.setStatus("PENDING_INITIAL_PAYMENT");
            }
            
            // If 100% paid, contract is completed
            if (contract.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0) {
                contract.setStatus("COMPLETED");
            } else if (contract.getContractEligible() && contract.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0) {
                contract.setStatus("PAYMENT_IN_PROGRESS");
            }
            
            contractRepository.save(contract);
        }
    }
    
    public Payment createPayment(String studentId, String installmentId, BigDecimal amount, 
            String channel, String phoneNumber, String payerName, String payerEmail, 
            String serviceCode, String transactionId) {
        
        Optional<StudentContract> contractOpt = contractRepository.findByStudentId(studentId);
        Optional<Installment> installmentOpt = installmentRepository.findByInstallmentId(installmentId);
        
        Payment payment = new Payment();
        payment.setPaymentId("PAY-" + System.currentTimeMillis());
        payment.setAmount(amount);
        payment.setOriginalAmount(amount);
        payment.setStatus("INITIATED");
        payment.setChannel(channel);
        payment.setPhoneNumber(phoneNumber);
        payment.setPayerName(payerName);
        payment.setPayerEmail(payerEmail);
        payment.setServiceCode(serviceCode);
        payment.setTransactionId(transactionId);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        
        contractOpt.ifPresent(payment::setContract);
        installmentOpt.ifPresent(payment::setInstallment);
        
        return paymentRepository.save(payment);
    }
    
    public List<Payment> getPaymentsByStudentId(String studentId) {
        return paymentRepository.findByContract_StudentId(studentId);
    }
}