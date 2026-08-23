package com.academix.document.entity;

import com.academix.grade.entity.Semester;
import com.academix.student.entity.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "administrative_documents")
public class AdministrativeDocument {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private DocumentType type;

	@Column(nullable = false, length = 20)
	private String academicYear;

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private Semester semester;

	@Column(nullable = false, length = 255)
	private String fileName;

	@Column(nullable = false, updatable = false)
	private Instant generatedAt;

	@Column(nullable = false, length = 255)
	private String generatedBy;

	public AdministrativeDocument() {
	}

	public AdministrativeDocument(Student student, DocumentType type, String academicYear, Semester semester,
			String fileName, String generatedBy) {
		this.student = student;
		this.type = type;
		this.academicYear = academicYear;
		this.semester = semester;
		this.fileName = fileName;
		this.generatedBy = generatedBy;
	}

	@PrePersist
	public void onCreate() {
		generatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Student getStudent() {
		return student;
	}

	public DocumentType getType() {
		return type;
	}

	public String getAcademicYear() {
		return academicYear;
	}

	public Semester getSemester() {
		return semester;
	}

	public String getFileName() {
		return fileName;
	}

	public Instant getGeneratedAt() {
		return generatedAt;
	}

	public String getGeneratedBy() {
		return generatedBy;
	}
}
