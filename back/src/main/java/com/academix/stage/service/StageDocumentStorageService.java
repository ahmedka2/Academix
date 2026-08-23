package com.academix.stage.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class StageDocumentStorageService {

	private final Path storageRoot;

	public StageDocumentStorageService(@Value("${app.stage-documents.directory:./stage-documents-storage}") String storageDirectory) {
		this.storageRoot = Path.of(storageDirectory).toAbsolutePath();
	}

	public String store(byte[] fileBytes, String suggestedName) {
		try {
			Files.createDirectories(storageRoot);
			String filename = UUID.randomUUID() + "-" + suggestedName;
			Files.write(storageRoot.resolve(filename), fileBytes);
			return filename;
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store file");
		}
	}

	public byte[] read(String filename) {
		Path path = storageRoot.resolve(filename);
		if (!Files.exists(path)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
		}
		try {
			return Files.readAllBytes(path);
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read file");
		}
	}

	public void delete(String filename) {
		try {
			Files.deleteIfExists(storageRoot.resolve(filename));
		} catch (IOException ignored) {
			// best-effort cleanup
		}
	}
}
