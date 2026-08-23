package com.academix.document.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class DocumentStorageService {

	private final Path documentsRoot;

	public DocumentStorageService(@Value("${app.documents.directory:./documents-storage}") String documentsDirectory) {
		this.documentsRoot = Path.of(documentsDirectory).toAbsolutePath();
	}

	public String store(byte[] pdfBytes, String suggestedName) {
		try {
			Files.createDirectories(documentsRoot);
			String filename = UUID.randomUUID() + "-" + suggestedName + ".pdf";
			Files.write(documentsRoot.resolve(filename), pdfBytes);
			return filename;
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store document");
		}
	}

	public byte[] read(String filename) {
		Path path = documentsRoot.resolve(filename);
		if (!Files.exists(path)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document file not found");
		}
		try {
			return Files.readAllBytes(path);
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read document");
		}
	}

	public void delete(String filename) {
		try {
			Files.deleteIfExists(documentsRoot.resolve(filename));
		} catch (IOException ignored) {
			// best-effort cleanup
		}
	}
}
