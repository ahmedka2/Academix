package com.academix.grade.repository;

import com.academix.grade.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GradeRepository extends JpaRepository<Grade, Long> {

	List<Grade> findByStudentId(Long studentId);

	List<Grade> findByStudentIdAndAcademicYear(Long studentId, String academicYear);
}
