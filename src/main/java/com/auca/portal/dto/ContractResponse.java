package com.auca.portal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ContractResponse {
    private String contractId;
    private String studentId;
    private String studentName;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private Double paymentProgress;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<InstallmentDto> installments;
    
    public String getContractId() { return contractId; }
    public void setContractId(String contractId) { this.contractId = contractId; }
    
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    
    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(BigDecimal remainingAmount) { this.remainingAmount = remainingAmount; }
    
    public Double getPaymentProgress() { return paymentProgress; }
    public void setPaymentProgress(Double paymentProgress) { this.paymentProgress = paymentProgress; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public List<InstallmentDto> getInstallments() { return installments; }
    public void setInstallments(List<InstallmentDto> installments) { this.installments = installments; }
    
    public static class InstallmentDto {
        private String installmentId;
        private Integer installmentNumber;
        private BigDecimal amount;
        private BigDecimal paidAmount;
        private LocalDate dueDate;
        private LocalDate paidDate;
        private String status;
        private String slipNumber;
        
        public String getInstallmentId() { return installmentId; }
        public void setInstallmentId(String installmentId) { this.installmentId = installmentId; }
        
        public Integer getInstallmentNumber() { return installmentNumber; }
        public void setInstallmentNumber(Integer installmentNumber) { this.installmentNumber = installmentNumber; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        
        public BigDecimal getPaidAmount() { return paidAmount; }
        public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
        
        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
        
        public LocalDate getPaidDate() { return paidDate; }
        public void setPaidDate(LocalDate paidDate) { this.paidDate = paidDate; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getSlipNumber() { return slipNumber; }
        public void setSlipNumber(String slipNumber) { this.slipNumber = slipNumber; }
    }
}