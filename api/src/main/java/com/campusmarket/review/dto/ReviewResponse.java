package com.campusmarket.review.dto;

import com.campusmarket.review.Review;
import com.campusmarket.review.ReviewDirection;

import java.time.LocalDateTime;

public record ReviewResponse(Long id, Long orderId, Long itemId, String itemTitle, int rating, String tags,
		String comment, ReviewDirection direction, Long reviewerId, String reviewerNickname, LocalDateTime createdAt) {

	public static ReviewResponse of(Review review, String itemTitle, String reviewerNickname) {
		return new ReviewResponse(review.getId(), review.getOrderId(), review.getItemId(), itemTitle,
				review.getRating(), review.getTags(), review.getComment(), review.getDirection(),
				review.getReviewerId(), reviewerNickname, review.getCreatedAt());
	}
}
