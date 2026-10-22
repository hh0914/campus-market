package com.campusmarket.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentVerificationRepository extends JpaRepository<StudentVerification, Long> {

	Optional<StudentVerification> findFirstByUserIdAndStatusOrderByIdDesc(Long userId, VerificationStatus status);
}
