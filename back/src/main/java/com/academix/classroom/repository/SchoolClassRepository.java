package com.academix.classroom.repository;

import com.academix.classroom.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

	boolean existsByLevelId(Long levelId);

	long countByLevelId(Long levelId);

	boolean existsByLevelIdAndNameIgnoreCase(Long levelId, String name);

	List<SchoolClass> findAllByOrderByLevelIdAscNameAsc();
}
