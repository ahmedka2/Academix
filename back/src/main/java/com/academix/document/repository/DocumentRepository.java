package com.academix.document.repository;

import com.academix.document.entity.AdministrativeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<AdministrativeDocument, Long> {

	List<AdministrativeDocument> findByStudentId(Long studentId);
}
