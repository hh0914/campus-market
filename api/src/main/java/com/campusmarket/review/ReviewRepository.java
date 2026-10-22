package com.campusmarket.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

	boolean existsByOrderIdAndReviewerId(Long orderId, Long reviewerId);

	List<Review> findByOrderIdOrderByIdAsc(Long orderId);

	Page<Review> findByRevieweeIdOrderByCreatedAtDesc(Long revieweeId, Pageable pageable);

	List<Review> findByRevieweeId(Long revieweeId);
}
