package com.academix.classroom.repository;

import com.academix.classroom.entity.TeacherAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, Long> {

	List<TeacherAssignment> findAllBySchoolClassId(Long schoolClassId);

	List<TeacherAssignment> findAllByTeacherId(Long teacherId);

	boolean existsByTeacherIdAndSchoolClassId(Long teacherId, Long schoolClassId);

	boolean existsByTeacherIdAndSchoolClassIdAndSubjectIgnoreCase(Long teacherId, Long schoolClassId, String subject);
}
