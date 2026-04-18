package com.auca.portal.repository;

import com.auca.portal.entity.StudentContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentContractRepository extends JpaRepository<StudentContract, Long> {
    Optional<StudentContract> findByStudentId(String studentId);
    Optional<StudentContract> findByContractId(String contractId);
}