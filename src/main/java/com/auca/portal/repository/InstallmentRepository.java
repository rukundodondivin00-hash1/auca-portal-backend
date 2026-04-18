package com.auca.portal.repository;

import com.auca.portal.entity.Installment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstallmentRepository extends JpaRepository<Installment, Long> {
    List<Installment> findByContract_StudentId(String studentId);
    Optional<Installment> findByInstallmentId(String installmentId);
    List<Installment> findByContract_ContractIdOrderByInstallmentNumberAsc(String contractId);
}