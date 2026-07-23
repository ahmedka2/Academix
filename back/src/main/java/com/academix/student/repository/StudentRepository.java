package com.academix.student.repository;

import com.academix.student.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

	Optional<Student> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByStudentIdentifierIgnoreCase(String studentIdentifier);

	boolean existsByCinIgnoreCase(String cin);

	long countBySchoolClassId(Long schoolClassId);

	List<Student> findAllBySchoolClassIdOrderByLastNameAscFirstNameAsc(Long schoolClassId);

	@Query("""
			select s from Student s
			left join s.schoolClass sc
			left join sc.level lvl
			where lower(s.firstName) like lower(concat('%', :query, '%'))
				or lower(s.lastName) like lower(concat('%', :query, '%'))
				or lower(s.email) like lower(concat('%', :query, '%'))
				or lower(s.studentIdentifier) like lower(concat('%', :query, '%'))
				or lower(s.cin) like lower(concat('%', :query, '%'))
				or lower(s.fieldOfStudy) like lower(concat('%', :query, '%'))
				or lower(sc.name) like lower(concat('%', :query, '%'))
				or lower(lvl.name) like lower(concat('%', :query, '%'))
			""")
	List<Student> search(@Param("query") String query);
}
