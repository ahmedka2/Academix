package com.academix.absence.repository;

import com.academix.absence.entity.Absence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {

	List<Absence> findByStudentId(Long studentId);
}
