package com.auca.portal.dto;

import java.math.BigDecimal;

public class PaymentInitiateRequest {
    private String payerCode;
    private String serviceCode;
    private BigDecimal amount;
    private String channelName;
    private String phoneNumber;
    private String payerNames;
    private String payerEmail;
    private String installmentId;
    
    public String getPayerCode() { return payerCode; }
    public void setPayerCode(String payerCode) { this.payerCode = payerCode; }
    
    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public String getPayerNames() { return payerNames; }
    public void setPayerNames(String payerNames) { this.payerNames = payerNames; }
    
    public String getPayerEmail() { return payerEmail; }
    public void setPayerEmail(String payerEmail) { this.payerEmail = payerEmail; }
    
    public String getInstallmentId() { return installmentId; }
    public void setInstallmentId(String installmentId) { this.installmentId = installmentId; }
}