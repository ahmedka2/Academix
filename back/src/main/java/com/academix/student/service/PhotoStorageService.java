package com.academix.student.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class PhotoStorageService {

	private final Path uploadsRoot;

	public PhotoStorageService(@Value("${app.uploads.directory:../front/public/profile_picture}") String uploadsDirectory) {
		this.uploadsRoot = Path.of(uploadsDirectory).toAbsolutePath();
	}

	public String store(Long studentId, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo file is required");
		}

		String extension = switch (file.getContentType()) {
			case "image/jpeg" -> "jpg";
			case "image/png" -> "png";
			case "image/webp" -> "webp";
			default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo must be a JPEG, PNG, or WEBP image");
		};

		try {
			Files.createDirectories(uploadsRoot);
			String filename = studentId + "-" + UUID.randomUUID() + "." + extension;
			file.transferTo(uploadsRoot.resolve(filename));
			return "/profile_picture/" + filename;
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store photo");
		}
	}
}
