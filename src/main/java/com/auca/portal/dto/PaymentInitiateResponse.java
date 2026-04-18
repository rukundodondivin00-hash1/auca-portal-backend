package com.auca.portal.dto;

public class PaymentInitiateResponse {
    private String responseCode;
    private String responseMessage;
    private String transactionId;
    private String checkoutUrl;
    private String message;
    
    public String getResponseCode() { return responseCode; }
    public void setResponseCode(String responseCode) { this.responseCode = responseCode; }
    
    public String getResponseMessage() { return responseMessage; }
    public void setResponseMessage(String responseMessage) { this.responseMessage = responseMessage; }
    
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    
    public String getCheckoutUrl() { return checkoutUrl; }
    public void setCheckoutUrl(String checkoutUrl) { this.checkoutUrl = checkoutUrl; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}