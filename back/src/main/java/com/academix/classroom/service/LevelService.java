package com.academix.classroom.service;

import com.academix.classroom.dto.LevelRequest;
import com.academix.classroom.dto.LevelResponse;
import com.academix.classroom.entity.Level;
import com.academix.classroom.repository.LevelRepository;
import com.academix.classroom.repository.SchoolClassRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class LevelService {

	private final LevelRepository levelRepository;
	private final SchoolClassRepository schoolClassRepository;

	public LevelService(LevelRepository levelRepository, SchoolClassRepository schoolClassRepository) {
		this.levelRepository = levelRepository;
		this.schoolClassRepository = schoolClassRepository;
	}

	@Transactional(readOnly = true)
	public List<LevelResponse> getAll() {
		return levelRepository.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	public LevelResponse create(LevelRequest request) {
		String name = request.name().trim();
		if (levelRepository.existsByNameIgnoreCase(name)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Level name already exists");
		}
		return toResponse(levelRepository.save(new Level(name)));
	}

	public LevelResponse update(Long id, LevelRequest request) {
		Level level = findLevel(id);
		String name = request.name().trim();
		if (!level.getName().equalsIgnoreCase(name) && levelRepository.existsByNameIgnoreCase(name)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Level name already exists");
		}
		level.setName(name);
		return toResponse(level);
	}

	public void delete(Long id) {
		Level level = findLevel(id);
		if (schoolClassRepository.existsByLevelId(id)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Remove this level's classes first");
		}
		levelRepository.delete(level);
	}

	private Level findLevel(Long id) {
		return levelRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Level not found"));
	}

	private LevelResponse toResponse(Level level) {
		return new LevelResponse(level.getId(), level.getName(), schoolClassRepository.countByLevelId(level.getId()));
	}
}
