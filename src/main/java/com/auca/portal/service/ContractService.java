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
            BigDecimal totalAmount, LocalDate startDate) {
        
        String contractId = "CON-" + studentId + "-" + System.currentTimeMillis();
        
        StudentContract contract = new StudentContract();
        contract.setContractId(contractId);
        contract.setStudentId(studentId);
        contract.setStudentName(studentName);
        contract.setStudentEmail(studentEmail);
        contract.setTotalAmount(totalAmount);
        contract.setPaidAmount(BigDecimal.ZERO);
        contract.setRemainingAmount(totalAmount);
        contract.setStartDate(startDate);
        contract.setEndDate(startDate.plusYears(1));
        contract.setStatus("ACTIVE");
        contract.setCreatedAt(LocalDateTime.now());
        contract.setUpdatedAt(LocalDateTime.now());
        
        contract = contractRepository.save(contract);
        
        createInstallments(contract, totalAmount);
        
        return contract;
    }
    
    private void createInstallments(StudentContract contract, BigDecimal totalAmount) {
        int numberOfInstallments = 4;
        BigDecimal installmentAmount = totalAmount.divide(BigDecimal.valueOf(numberOfInstallments), 2, BigDecimal.ROUND_HALF_UP);
        
        LocalDate startDate = contract.getStartDate();
        
        for (int i = 1; i <= numberOfInstallments; i++) {
            Installment installment = new Installment();
            installment.setInstallmentId("INST-" + contract.getStudentId() + "-" + i);
            installment.setContract(contract);
            installment.setInstallmentNumber(i);
            installment.setAmount(installmentAmount);
            installment.setPaidAmount(BigDecimal.ZERO);
            installment.setDueDate(startDate.plusMonths((i - 1) * 3));
            installment.setStatus("PENDING");
            installment.setCreatedAt(LocalDateTime.now());
            installment.setUpdatedAt(LocalDateTime.now());
            
            installmentRepository.save(installment);
        }
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
                    installment.setUpdatedAt(LocalDateTime.now());
                    installmentRepository.save(installment);
                    
                    // Update contract
                    updateContractPaidAmount(installment.getContract().getStudentId());
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
                .map(Installment::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            contract.setPaidAmount(totalPaid);
            contract.setRemainingAmount(contract.getTotalAmount().subtract(totalPaid));
            contract.setUpdatedAt(LocalDateTime.now());
            
            // Check if contract is VALID (at least 50% paid)
            BigDecimal halfAmount = contract.getTotalAmount().divide(BigDecimal.valueOf(2));
            
            if (contract.getPaidAmount().compareTo(halfAmount) >= 0) {
                contract.setStatus("VALID");
            } else if (contract.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0) {
                contract.setStatus("COMPLETED");
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