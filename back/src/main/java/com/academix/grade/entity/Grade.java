package com.academix.grade.entity;

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

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "grades")
public class Grade {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@Column(nullable = false, length = 100)
	private String subject;

	@Column(nullable = false, precision = 4, scale = 2)
	private BigDecimal score;

	@Column(nullable = false, precision = 4, scale = 2)
	private BigDecimal coefficient;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Semester semester;

	@Column(nullable = false, length = 20)
	private String academicYear;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 15)
	private GradeStatus status = GradeStatus.PENDING;

	@Column(length = 255)
	private String submittedBy;

	@Column(length = 500)
	private String reviewComment;

	private Instant reviewedAt;

	@Column(length = 255)
	private String reviewedBy;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	public Grade() {
	}

	public Grade(Student student, String subject, BigDecimal score, BigDecimal coefficient, Semester semester,
			String academicYear) {
		this.student = student;
		this.subject = subject;
		this.score = score;
		this.coefficient = coefficient;
		this.semester = semester;
		this.academicYear = academicYear;
	}

	@PrePersist
	public void onCreate() {
		createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Student getStudent() {
		return student;
	}

	public void setStudent(Student student) {
		this.student = student;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public BigDecimal getScore() {
		return score;
	}

	public void setScore(BigDecimal score) {
		this.score = score;
	}

	public BigDecimal getCoefficient() {
		return coefficient;
	}

	public void setCoefficient(BigDecimal coefficient) {
		this.coefficient = coefficient;
	}

	public Semester getSemester() {
		return semester;
	}

	public void setSemester(Semester semester) {
		this.semester = semester;
	}

	public String getAcademicYear() {
		return academicYear;
	}

	public void setAcademicYear(String academicYear) {
		this.academicYear = academicYear;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public GradeStatus getStatus() {
		return status;
	}

	public void setStatus(GradeStatus status) {
		this.status = status;
	}

	public String getSubmittedBy() {
		return submittedBy;
	}

	public void setSubmittedBy(String submittedBy) {
		this.submittedBy = submittedBy;
	}

	public String getReviewComment() {
		return reviewComment;
	}

	public void setReviewComment(String reviewComment) {
		this.reviewComment = reviewComment;
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
}
