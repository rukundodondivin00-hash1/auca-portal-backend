package com.auca.portal.service;

import com.auca.portal.entity.Installment;
import com.auca.portal.repository.InstallmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PaymentReminderScheduler {
    
    @Autowired
    private InstallmentRepository installmentRepository;
    
    @Autowired
    private EmailNotificationService emailNotificationService;
    
    /**
     * Run every day at 10:00 AM to check for payment reminders and overdue notifications
     * Cron: 0 10 * * * (daily at 10 AM)
     */
    @Scheduled(cron = "0 10 * * *")
    public void checkPaymentReminders() {
        // Get all pending installments
        List<Installment> pendingInstallments = installmentRepository.findByStatus("PENDING");
        emailNotificationService.checkAndSendReminders(pendingInstallments);
    }
    
    /**
     * Run every Monday at 8:00 AM for weekly payment status check
     */
    @Scheduled(cron = "0 8 * * 1")
    public void weeklyPaymentCheck() {
        System.out.println("Running weekly payment status check...");
        List<Installment> pendingInstallments = installmentRepository.findByStatus("PENDING");
        emailNotificationService.checkAndSendReminders(pendingInstallments);
    }
}
