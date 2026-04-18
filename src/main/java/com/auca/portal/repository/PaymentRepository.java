package com.auca.portal.repository;

import com.auca.portal.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByContract_StudentId(String studentId);
    List<Payment> findByInstallment_InstallmentId(String installmentId);
    Optional<Payment> findByTransactionId(String transactionId);
    Optional<Payment> findByPaymentId(String paymentId);
}