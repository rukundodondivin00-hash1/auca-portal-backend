package com.auca.portal.service;

import com.auca.portal.entity.Installment;
import com.auca.portal.entity.StudentContract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class EmailNotificationService {
    
    @Value("${spring.mail.username:}")
    private String fromEmail;
    
    @Value("${auca.notification.email:notifications@auca.ac.rw}")
    private String campusNotificationEmail;
    
    private final JavaMailSender mailSender;
    
    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    /**
     * Send payment reminder email to student
     */
    public void sendPaymentReminder(StudentContract contract, Installment installment, int daysUntilDue) {
        String emailAddress = contract.getNotificationEmail();
        if (emailAddress == null || emailAddress.isEmpty()) {
            emailAddress = contract.getStudentEmail(); // Fallback to student email
        }
        
        if (emailAddress == null || emailAddress.isEmpty()) {
            return;
        }
        
        String subject = "Payment Reminder - AUCA Student Portal";
        String message = String.format(
            "Dear %s,\n\n" +
            "This is a reminder that your payment installment is due soon.\n\n" +
            "Installment Details:\n" +
            "- Installment #%d\n" +
            "- Amount: RWF %s\n" +
            "- Due Date: %s\n" +
            "- Days Remaining: %d\n\n" +
            "Please make your payment on time to avoid late payment penalties (5%% on remaining balance).\n\n" +
            "Best regards,\nAUCA Student Portal",
            contract.getStudentName(),
            installment.getInstallmentNumber(),
            installment.getAmount(),
            installment.getDueDate(),
            daysUntilDue
        );
        
        sendEmail(emailAddress, subject, message);
    }
    
    /**
     * Send payment overdue notification
     */
    public void sendOverdueNotification(StudentContract contract, Installment installment, int daysOverdue) {
        String emailAddress = contract.getNotificationEmail();
        if (emailAddress == null || emailAddress.isEmpty()) {
            emailAddress = contract.getStudentEmail(); // Fallback to student email
        }
        
        if (emailAddress == null || emailAddress.isEmpty()) {
            return;
        }
        
        String subject = "URGENT: Payment Overdue - AUCA Student Portal";
        String message = String.format(
            "Dear %s,\n\n" +
            "Your payment is now OVERDUE!\n\n" +
            "Installment Details:\n" +
            "- Installment #%d\n" +
            "- Amount: RWF %s\n" +
            "- Due Date: %s\n" +
            "- Days Overdue: %d\n\n" +
            "IMPORTANT: A 5%% late payment penalty has been applied to your remaining balance.\n" +
            "Please make your payment immediately to avoid further penalties and contract suspension.\n\n" +
            "Best regards,\nAUCA Student Portal",
            contract.getStudentName(),
            installment.getInstallmentNumber(),
            installment.getAmount(),
            installment.getDueDate(),
            daysOverdue
        );
        
        sendEmail(emailAddress, subject, message);
    }
    
    /**
     * Send contract eligibility confirmation email
     */
    public void sendContractEligibilityEmail(StudentContract contract) {
        String emailAddress = contract.getNotificationEmail();
        if (emailAddress == null || emailAddress.isEmpty()) {
            emailAddress = contract.getStudentEmail(); // Fallback to student email
        }
        
        if (emailAddress == null || emailAddress.isEmpty()) {
            return;
        }
        
        String subject = "Congratulations! Your Contract is Now Eligible - AUCA Student Portal";
        String message = String.format(
            "Dear %s,\n\n" +
            "Congratulations! You have successfully paid 50%% of your tuition fees.\n\n" +
            "Your contract is now ELIGIBLE and you can proceed with enrollment.\n\n" +
            "Contract Details:\n" +
            "- Contract ID: %s\n" +
            "- Total Amount: RWF %s\n" +
            "- Amount Paid: RWF %s\n" +
            "- Remaining Balance: RWF %s\n\n" +
            "Remaining Payment Schedule:\n" +
            "- Installment #2: Due on Month 2 (25%% - RWF %s)\n" +
            "- Installment #3: Due on Month 3 (25%% - RWF %s)\n\n" +
            "Best regards,\nAUCA Student Portal",
            contract.getStudentName(),
            contract.getContractId(),
            contract.getTotalAmount(),
            contract.getPaidAmount(),
            contract.getRemainingAmount(),
            contract.getTotalAmount().multiply(BigDecimal.valueOf(0.25)),
            contract.getTotalAmount().multiply(BigDecimal.valueOf(0.25))
        );
        
        sendEmail(emailAddress, subject, message);
    }
    
    /**
     * Send payment completion email
     */
    public void sendPaymentCompletionEmail(StudentContract contract) {
        String emailAddress = contract.getNotificationEmail();
        if (emailAddress == null || emailAddress.isEmpty()) {
            emailAddress = contract.getStudentEmail(); // Fallback to student email
        }
        
        if (emailAddress == null || emailAddress.isEmpty()) {
            return;
        }
        
        String subject = "Payment Complete - AUCA Student Portal";
        String message = String.format(
            "Dear %s,\n\n" +
            "Thank you! Your tuition fees have been fully paid.\n\n" +
            "Contract Details:\n" +
            "- Contract ID: %s\n" +
            "- Total Amount: RWF %s\n" +
            "- Status: COMPLETED\n\n" +
            "Your enrollment is now complete. Welcome to AUCA!\n\n" +
            "Best regards,\nAUCA Student Portal",
            contract.getStudentName(),
            contract.getContractId(),
            contract.getTotalAmount()
        );
        
        sendEmail(emailAddress, subject, message);
    }
    
    /**
     * Generic email sender method
     */
    private void sendEmail(String to, String subject, String message) {
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(to);
            email.setSubject(subject);
            email.setText(message);
            email.setFrom(fromEmail);
            
            // Also CC the campus notification email for record-keeping
            email.setCc(campusNotificationEmail);
            
            // Note: This requires proper mail configuration in application.yml
            // Uncomment when mail server is configured:
            // mailSender.send(email);
            
            // For now, log that email would be sent
            System.out.println("Email would be sent to: " + to);
            System.out.println("Subject: " + subject);
        } catch (Exception e) {
            System.err.println("Error sending email to " + to + ": " + e.getMessage());
        }
    }
    
    /**
     * Check and send payment reminders for upcoming due dates
     * Call this periodically (via scheduled task)
     */
    public void checkAndSendReminders(List<Installment> pendingInstallments) {
        LocalDate today = LocalDate.now();
        
        for (Installment installment : pendingInstallments) {
            if (installment.getDueDate() != null) {
                long daysUntilDue = ChronoUnit.DAYS.between(today, installment.getDueDate());
                
                // Send reminder 3 days before due date
                if (daysUntilDue == 3) {
                    sendPaymentReminder(installment.getContract(), installment, (int) daysUntilDue);
                }
                
                // Send overdue notification if passed due date
                if (daysUntilDue < 0) {
                    long daysOverdue = Math.abs(daysUntilDue);
                    sendOverdueNotification(installment.getContract(), installment, (int) daysOverdue);
                }
            }
        }
    }
}
