package com.campusmarket.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

	Optional<User> findByStudentNo(String studentNo);

	Optional<User> findByEmail(String email);

	boolean existsByStudentNo(String studentNo);

	boolean existsByEmail(String email);

	@Query("select count(u) from User u where u.createdAt >= :from")
	long countCreatedAfter(@Param("from") java.time.LocalDateTime from);

	java.util.List<User> findByCreatedAtAfter(java.time.LocalDateTime from);

	long countByRole(Role role);
}
