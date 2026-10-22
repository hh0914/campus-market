package com.campusmarket.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	long countByUserIdAndReadFlagFalse(Long userId);

	@Modifying
	@Query("update Notification n set n.readFlag = true where n.userId = :userId and n.readFlag = false")
	int markAllRead(@Param("userId") Long userId);
}
