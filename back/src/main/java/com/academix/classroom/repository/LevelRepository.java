package com.academix.classroom.repository;

import com.academix.classroom.entity.Level;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LevelRepository extends JpaRepository<Level, Long> {

	boolean existsByNameIgnoreCase(String name);
}
