package com.academix.classroom.entity;

import com.academix.auth.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "teacher_assignments", uniqueConstraints = {
		@UniqueConstraint(name = "uk_teacher_assignments", columnNames = {"teacher_id", "class_id", "subject"})
})
public class TeacherAssignment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "teacher_id", nullable = false)
	private User teacher;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "class_id", nullable = false)
	private SchoolClass schoolClass;

	@Column(nullable = false, length = 100)
	private String subject;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	public TeacherAssignment() {
	}

	public TeacherAssignment(User teacher, SchoolClass schoolClass, String subject) {
		this.teacher = teacher;
		this.schoolClass = schoolClass;
		this.subject = subject;
	}

	@PrePersist
	public void onCreate() {
		createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public User getTeacher() {
		return teacher;
	}

	public SchoolClass getSchoolClass() {
		return schoolClass;
	}

	public String getSubject() {
		return subject;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
