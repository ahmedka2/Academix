package com.academix.student.repository;

import com.academix.auth.entity.Role;
import com.academix.auth.entity.User;
import com.academix.classroom.entity.Level;
import com.academix.classroom.entity.SchoolClass;
import com.academix.student.entity.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository slice test: boots JPA + an in-memory H2 database only (no web layer,
 * no MySQL). This is the level that catches JPQL mistakes, which no mock ever can.
 *
 * The regression it guards: StudentRepository.search uses an explicit
 * "left join s.schoolClass sc" because implicit path navigation (s.schoolClass.name)
 * silently becomes an INNER join in JPQL, which would drop every unassigned student
 * from ALL search results, not just class-name searches.
 */
@DataJpaTest
class StudentRepositorySearchTest {

	@Autowired
	private StudentRepository studentRepository;

	@Autowired
	private TestEntityManager entityManager;

	private Student assigned;
	private Student unassigned;

	@BeforeEach
	void seed() {
		Level level = entityManager.persist(new Level("Deuxieme annee"));
		SchoolClass classA = entityManager.persist(new SchoolClass(level, "2A-INFO"));

		assigned = persistStudent("Sarra", "Trabelsi", "sarra@academix.com", "11111111", "100academix001");
		assigned.setSchoolClass(classA);

		unassigned = persistStudent("Mehdi", "Gharbi", "mehdi@academix.com", "22222222", "100academix002");
		unassigned.setSchoolClass(null);

		entityManager.flush();
	}

	private Student persistStudent(String firstName, String lastName, String email, String cin, String identifier) {
		User user = entityManager.persist(new User(firstName + " " + lastName, email, "hash", Role.STUDENT));
		return entityManager.persist(new Student(user, firstName, lastName, cin, identifier, email,
				null, "Informatique", null, null));
	}

	@Test
	void searchByNameStillFindsStudentsWithNoClassAssigned() {
		List<Student> results = studentRepository.search("Mehdi");

		assertThat(results).extracting(Student::getEmail).containsExactly("mehdi@academix.com");
	}

	@Test
	void searchMatchesOnClassName() {
		List<Student> results = studentRepository.search("2A-INFO");

		assertThat(results).extracting(Student::getEmail).containsExactly("sarra@academix.com");
	}

	@Test
	void searchMatchesOnLevelName() {
		List<Student> results = studentRepository.search("Deuxieme");

		assertThat(results).extracting(Student::getEmail).containsExactly("sarra@academix.com");
	}

	@Test
	void searchIsCaseInsensitive() {
		assertThat(studentRepository.search("sarra")).hasSize(1);
		assertThat(studentRepository.search("SARRA")).hasSize(1);
	}

	@Test
	void searchOnASharedFieldReturnsAssignedAndUnassignedStudentsAlike() {
		List<Student> results = studentRepository.search("Informatique");

		assertThat(results).extracting(Student::getEmail)
				.containsExactlyInAnyOrder("sarra@academix.com", "mehdi@academix.com");
	}

	@Test
	void countBySchoolClassIdOnlyCountsThatClass() {
		Long classId = assigned.getSchoolClass().getId();

		assertThat(studentRepository.countBySchoolClassId(classId)).isEqualTo(1);
		assertThat(unassigned.getSchoolClass()).isNull();
	}
}
