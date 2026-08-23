package com.academix.stage.service;

import com.academix.stage.dto.StageDocumentResponse;
import com.academix.stage.dto.ValidateStageDocumentRequest;
import com.academix.stage.entity.StageDocument;
import com.academix.stage.entity.StageDocumentStatus;
import com.academix.stage.entity.StageDocumentType;
import com.academix.stage.repository.StageDocumentRepository;
import com.academix.student.entity.Student;
import com.academix.student.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class StageDocumentService {

	private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("application/pdf", "image/jpeg", "image/png");

	private final StageDocumentRepository stageDocumentRepository;
	private final StudentRepository studentRepository;
	private final StageDocumentStorageService storageService;

	public StageDocumentService(StageDocumentRepository stageDocumentRepository, StudentRepository studentRepository,
			StageDocumentStorageService storageService) {
		this.stageDocumentRepository = stageDocumentRepository;
		this.studentRepository = studentRepository;
		this.storageService = storageService;
	}

	public StageDocumentResponse upload(StageDocumentType type, MultipartFile file) {
		Student student = currentStudent();
		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
		}
		if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be a PDF, JPEG, or PNG");
		}
		if (file.getSize() > MAX_FILE_SIZE) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be smaller than 10MB");
		}

		StageDocument document = stageDocumentRepository.findByStudentIdAndType(student.getId(), type)
				.orElseGet(() -> new StageDocument(student, type));
		if (document.getFileName() != null) {
			storageService.delete(document.getFileName());
		}

		String suggestedName = student.getStudentIdentifier() + "-" + type.name().toLowerCase() + "-" + safeExtension(file);
		String fileName = storageService.store(readBytes(file), suggestedName);

		document.setFileName(fileName);
		document.setStatus(StageDocumentStatus.PENDING);
		document.setComment(null);
		document.setReviewedAt(null);
		document.setReviewedBy(null);
		document.setUploadedAt(Instant.now());
		return toResponse(stageDocumentRepository.save(document));
	}

	@Transactional(readOnly = true)
	public List<StageDocumentResponse> getMine() {
		Student student = currentStudent();
		return stageDocumentRepository.findByStudentId(student.getId()).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<StageDocumentResponse> getAll(Long studentId, StageDocumentStatus status) {
		requireAdministration();
		List<StageDocument> documents = studentId != null
				? stageDocumentRepository.findByStudentId(studentId)
				: stageDocumentRepository.findAll();
		return documents.stream()
				.filter(document -> status == null || document.getStatus() == status)
				.sorted(Comparator.comparing(StageDocument::getUploadedAt).reversed())
				.map(this::toResponse)
				.toList();
	}

	public record DownloadedFile(String fileName, byte[] content) {
	}

	@Transactional(readOnly = true)
	public DownloadedFile download(Long id) {
		StageDocument document = findDocument(id);
		if (!hasRole("ROLE_ADMINISTRATION")) {
			Student student = currentStudent();
			if (!document.getStudent().getId().equals(student.getId())) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot download this document");
			}
		}
		return new DownloadedFile(document.getFileName(), storageService.read(document.getFileName()));
	}

	public StageDocumentResponse validate(Long id, ValidateStageDocumentRequest request) {
		requireAdministration();
		StageDocument document = findDocument(id);
		if (document.getStatus() != StageDocumentStatus.PENDING) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This document is not pending review");
		}
		document.setStatus(request.approved() ? StageDocumentStatus.APPROVED : StageDocumentStatus.REJECTED);
		document.setComment(normalizeOptional(request.comment()));
		document.setReviewedAt(Instant.now());
		document.setReviewedBy(currentUsername());
		return toResponse(document);
	}

	public void delete(Long id) {
		requireAdministration();
		StageDocument document = findDocument(id);
		storageService.delete(document.getFileName());
		stageDocumentRepository.delete(document);
	}

	private String safeExtension(MultipartFile file) {
		return switch (file.getContentType()) {
			case "application/pdf" -> "pdf";
			case "image/jpeg" -> "jpg";
			case "image/png" -> "png";
			default -> "bin";
		};
	}

	private byte[] readBytes(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read uploaded file");
		}
	}

	private String normalizeOptional(String value) {
		if (value == null || value.trim().isBlank()) {
			return null;
		}
		return value.trim();
	}

	private StageDocumentResponse toResponse(StageDocument document) {
		Student student = document.getStudent();
		return new StageDocumentResponse(
				document.getId(),
				student.getId(),
				student.getFirstName() + " " + student.getLastName(),
				student.getStudentIdentifier(),
				document.getType(),
				document.getStatus(),
				document.getComment(),
				document.getUploadedAt(),
				document.getReviewedAt(),
				document.getReviewedBy());
	}

	private StageDocument findDocument(Long id) {
		return stageDocumentRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
	}

	private Student currentStudent() {
		return studentRepository.findByEmailIgnoreCase(currentUsername())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found"));
	}

	private void requireAdministration() {
		if (!hasRole("ROLE_ADMINISTRATION")) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administration access required");
		}
	}

	private boolean hasRole(String role) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals(role));
	}

	private String currentUsername() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getName() == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
		}
		return authentication.getName();
	}
}
