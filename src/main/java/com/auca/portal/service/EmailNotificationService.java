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

    public void sendPaymentReminder(StudentContract contract, Installment installment, int daysUntilDue) {
        if (contract == null || installment == null) return;

        String emailAddress = getEmail(contract);
        if (emailAddress == null) return;

        String subject = "Payment Reminder - AUCA Student Portal";

        String message = String.format(
            "Dear %s,\n\nYour payment is due soon...\nInstallment #%d\nAmount: RWF %s\nDue Date: %s\nDays Remaining: %d\n\nAUCA Portal",
            contract.getStudentName(),
            installment.getInstallmentNumber(),
            installment.getAmount(),
            installment.getDueDate(),
            daysUntilDue
        );

        sendEmail(emailAddress, subject, message);
    }

    public void sendOverdueNotification(StudentContract contract, Installment installment, int daysOverdue) {
        if (contract == null || installment == null) return;

        String emailAddress = getEmail(contract);
        if (emailAddress == null) return;

        String subject = "URGENT: Payment Overdue - AUCA";

        String message = String.format(
            "Dear %s,\n\nYour payment is OVERDUE!\nInstallment #%d\nAmount: RWF %s\nDays Overdue: %d\n\nAUCA Portal",
            contract.getStudentName(),
            installment.getInstallmentNumber(),
            installment.getAmount(),
            daysOverdue
        );

        sendEmail(emailAddress, subject, message);
    }

    public void sendContractEligibilityEmail(StudentContract contract) {
        if (contract == null) return;

        String emailAddress = getEmail(contract);
        if (emailAddress == null) return;

        String subject = "Contract Eligible - AUCA";

        String message = String.format(
            "Dear %s,\n\nYou are now eligible for enrollment.\nContract ID: %s\nRemaining Balance: RWF %s\n\nAUCA Portal",
            contract.getStudentName(),
            contract.getContractId(),
            contract.getRemainingAmount()
        );

        sendEmail(emailAddress, subject, message);
    }

    public void sendPaymentCompletionEmail(StudentContract contract) {
        if (contract == null) return;

        String emailAddress = getEmail(contract);
        if (emailAddress == null) return;

        String subject = "Payment Complete - AUCA";

        String message = String.format(
            "Dear %s,\n\nYour payment is complete.\nContract ID: %s\n\nWelcome to AUCA!",
            contract.getStudentName(),
            contract.getContractId()
        );

        sendEmail(emailAddress, subject, message);
    }

    private String getEmail(StudentContract contract) {
        String email = contract.getNotificationEmail();
        if (email == null || email.isEmpty()) {
            email = contract.getStudentEmail();
        }
        return (email == null || email.isEmpty()) ? null : email;
    }

    private void sendEmail(String to, String subject, String message) {
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(to);
            email.setSubject(subject);
            email.setText(message);

            if (fromEmail != null && !fromEmail.isEmpty()) {
                email.setFrom(fromEmail);
            }

            email.setCc(campusNotificationEmail);

            // ENABLE ONLY WHEN MAIL IS CONFIGURED
            // mailSender.send(email);

            System.out.println("EMAIL TO: " + to);
            System.out.println("SUBJECT: " + subject);

        } catch (Exception e) {
            System.err.println("Email failed: " + e.getMessage());
        }
    }

    public void checkAndSendReminders(List<Installment> pendingInstallments) {
        if (pendingInstallments == null) return;

        for (Installment installment : pendingInstallments) {
            if (installment == null || installment.getDueDate() == null) continue;

            long days = ChronoUnit.DAYS.between(LocalDate.now(), installment.getDueDate());

            if (days == 3) {
                sendPaymentReminder(installment.getContract(), installment, 3);
            }

            if (days < 0) {
                sendOverdueNotification(
                    installment.getContract(),
                    installment,
                    (int) Math.abs(days)
                );
            }
        }
    }
}