package com.campusmarket.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
		@Min(value = 1, message = "评分为 1-5 分")
		@Max(value = 5, message = "评分为 1-5 分")
		int rating,

		@Size(max = 200, message = "标签最多 200 个字符")
		String tags,

		@Size(max = 500, message = "评价内容最多 500 个字符")
		String comment) {
}
