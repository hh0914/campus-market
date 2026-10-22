package com.campusmarket.moderation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

	Page<Report> findByStatusOrderByCreatedAtAsc(ReportStatus status, Pageable pageable);

	Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);

	Page<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId, Pageable pageable);

	long countByStatus(ReportStatus status);

	long countByTargetTypeAndTargetIdAndStatus(ReportTargetType targetType, Long targetId, ReportStatus status);
}
