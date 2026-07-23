package com.academix.auth.repository;

import com.academix.auth.entity.Role;
import com.academix.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	List<User> findAllByRole(Role role);

	Optional<User> findByIdAndRole(Long id, Role role);
}
