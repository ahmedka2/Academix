package com.academix.stage.repository;

import com.academix.stage.entity.StageDocument;
import com.academix.stage.entity.StageDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StageDocumentRepository extends JpaRepository<StageDocument, Long> {

	List<StageDocument> findByStudentId(Long studentId);

	Optional<StageDocument> findByStudentIdAndType(Long studentId, StageDocumentType type);
}
