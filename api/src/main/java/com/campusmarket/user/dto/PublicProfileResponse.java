package com.campusmarket.user.dto;

import com.campusmarket.auth.dto.UserSummary;
import com.campusmarket.review.Review;

import java.time.LocalDateTime;

public record PublicProfileResponse(
		UserSummary user,
		ReputationResponse reputation,
		long onSaleItems,
		boolean verified,
		java.util.List<RecentReview> recentReviews) {

	public record RecentReview(Long id, int rating, String tags, String comment, String reviewerNickname,
			LocalDateTime createdAt) {

		public static RecentReview of(Review review, String reviewerNickname) {
			return new RecentReview(review.getId(), review.getRating(), review.getTags(), review.getComment(),
					reviewerNickname, review.getCreatedAt());
		}
	}
}
