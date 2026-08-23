package com.academix.stage.entity;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "stage_documents", uniqueConstraints = {
		@UniqueConstraint(name = "uk_stage_documents_student_type", columnNames = {"student_id", "type"})
})
public class StageDocument {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private StageDocumentType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 15)
	private StageDocumentStatus status = StageDocumentStatus.PENDING;

	@Column(nullable = false, length = 255)
	private String fileName;

	@Column(length = 500)
	private String comment;

	@Column(nullable = false)
	private Instant uploadedAt;

	private Instant reviewedAt;

	@Column(length = 255)
	private String reviewedBy;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	public StageDocument() {
	}

	public StageDocument(Student student, StageDocumentType type) {
		this.student = student;
		this.type = type;
	}

	@PrePersist
	public void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	public void onUpdate() {
		updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Student getStudent() {
		return student;
	}

	public StageDocumentType getType() {
		return type;
	}

	public StageDocumentStatus getStatus() {
		return status;
	}

	public void setStatus(StageDocumentStatus status) {
		this.status = status;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Instant getUploadedAt() {
		return uploadedAt;
	}

	public void setUploadedAt(Instant uploadedAt) {
		this.uploadedAt = uploadedAt;
	}

	public Instant getReviewedAt() {
		return reviewedAt;
	}

	public void setReviewedAt(Instant reviewedAt) {
		this.reviewedAt = reviewedAt;
	}

	public String getReviewedBy() {
		return reviewedBy;
	}

	public void setReviewedBy(String reviewedBy) {
		this.reviewedBy = reviewedBy;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
