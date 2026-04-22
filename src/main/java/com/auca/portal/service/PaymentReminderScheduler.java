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
     * Daily payment reminder check (10:00 AM)
     */
    @Scheduled(cron = "0 0 10 * * ?")
    public void checkPaymentReminders() {
        try {
            List<Installment> pendingInstallments =
                    installmentRepository.findByStatus("PENDING");

            if (pendingInstallments == null || pendingInstallments.isEmpty()) {
                System.out.println("No pending installments found for daily check.");
                return;
            }

            emailNotificationService.checkAndSendReminders(pendingInstallments);

            System.out.println("Daily payment reminder job executed successfully.");

        } catch (Exception e) {
            System.err.println("Error in daily payment reminder job: " + e.getMessage());
        }
    }

    /**
     * Weekly payment status check (Monday 8:00 AM)
     */
    @Scheduled(cron = "0 0 8 ? * MON")
    public void weeklyPaymentCheck() {
        try {
            System.out.println("Running weekly payment status check...");

            List<Installment> pendingInstallments =
                    installmentRepository.findByStatus("PENDING");

            if (pendingInstallments == null || pendingInstallments.isEmpty()) {
                System.out.println("No pending installments found for weekly check.");
                return;
            }

            emailNotificationService.checkAndSendReminders(pendingInstallments);

            System.out.println("Weekly payment check completed successfully.");

        } catch (Exception e) {
            System.err.println("Error in weekly payment check: " + e.getMessage());
        }
    }
}