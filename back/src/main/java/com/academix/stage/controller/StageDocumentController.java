package com.academix.stage.controller;

import com.academix.stage.dto.StageDocumentResponse;
import com.academix.stage.dto.ValidateStageDocumentRequest;
import com.academix.stage.entity.StageDocumentStatus;
import com.academix.stage.entity.StageDocumentType;
import com.academix.stage.service.StageDocumentService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/stage-documents")
public class StageDocumentController {

	private final StageDocumentService stageDocumentService;

	public StageDocumentController(StageDocumentService stageDocumentService) {
		this.stageDocumentService = stageDocumentService;
	}

	@PostMapping("/me/{type}")
	public ResponseEntity<StageDocumentResponse> upload(@PathVariable StageDocumentType type, @RequestPart("file") MultipartFile file) {
		return ResponseEntity.status(HttpStatus.CREATED).body(stageDocumentService.upload(type, file));
	}

	@GetMapping("/me")
	public ResponseEntity<List<StageDocumentResponse>> getMine() {
		return ResponseEntity.ok(stageDocumentService.getMine());
	}

	@GetMapping
	public ResponseEntity<List<StageDocumentResponse>> getAll(
			@RequestParam(required = false) Long studentId,
			@RequestParam(required = false) StageDocumentStatus status) {
		return ResponseEntity.ok(stageDocumentService.getAll(studentId, status));
	}

	@GetMapping("/{id}/download")
	public ResponseEntity<byte[]> download(@PathVariable Long id) {
		StageDocumentService.DownloadedFile file = stageDocumentService.download(id);
		ContentDisposition disposition = ContentDisposition.attachment().filename(file.fileName()).build();
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
				.body(file.content());
	}

	@PutMapping("/{id}/validate")
	public ResponseEntity<StageDocumentResponse> validate(@PathVariable Long id, @Valid @RequestBody ValidateStageDocumentRequest request) {
		return ResponseEntity.ok(stageDocumentService.validate(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		stageDocumentService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
