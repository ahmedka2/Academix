package com.academix.document.controller;

import com.academix.document.dto.DocumentResponse;
import com.academix.document.dto.GenerateDocumentRequest;
import com.academix.document.service.DocumentService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

	private final DocumentService documentService;

	public DocumentController(DocumentService documentService) {
		this.documentService = documentService;
	}

	@GetMapping
	public ResponseEntity<List<DocumentResponse>> getAll(@RequestParam(required = false) Long studentId) {
		return ResponseEntity.ok(documentService.getAll(studentId));
	}

	@GetMapping("/me")
	public ResponseEntity<List<DocumentResponse>> getMine() {
		return ResponseEntity.ok(documentService.getMine());
	}

	@PostMapping("/generate")
	public ResponseEntity<DocumentResponse> generate(@Valid @RequestBody GenerateDocumentRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(documentService.generate(request));
	}

	@GetMapping("/{id}/download")
	public ResponseEntity<byte[]> download(@PathVariable Long id) {
		DocumentService.DownloadedFile file = documentService.download(id);
		ContentDisposition disposition = ContentDisposition.attachment().filename(file.fileName()).build();
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
				.body(file.content());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		documentService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
