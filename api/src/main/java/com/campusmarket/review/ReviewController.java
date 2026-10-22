package com.campusmarket.review;

import com.campusmarket.auth.CurrentUser;
import com.campusmarket.common.ApiResponse;
import com.campusmarket.common.PageResult;
import com.campusmarket.review.dto.CreateReviewRequest;
import com.campusmarket.review.dto.ReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "评价", description = "订单双向评价")
@RestController
@RequestMapping("/api/v1")
public class ReviewController {

	private final ReviewService reviewService;
	private final CurrentUser currentUser;

	public ReviewController(ReviewService reviewService, CurrentUser currentUser) {
		this.reviewService = reviewService;
		this.currentUser = currentUser;
	}

	@Operation(summary = "提交评价")
	@PostMapping("/orders/{id}/reviews")
	public ApiResponse<ReviewResponse> create(@PathVariable Long id,
			@Valid @RequestBody CreateReviewRequest request) {
		return ApiResponse.ok(reviewService.create(currentUser.requireId(), id, request));
	}

	@Operation(summary = "订单的评价")
	@GetMapping("/orders/{id}/reviews")
	public ApiResponse<List<ReviewResponse>> byOrder(@PathVariable Long id) {
		return ApiResponse.ok(reviewService.byOrder(currentUser.requireId(), id));
	}

	@Operation(summary = "用户收到的评价")
	@GetMapping("/users/{id}/reviews")
	public ApiResponse<PageResult<ReviewResponse>> byUser(@PathVariable Long id,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size) {
		return ApiResponse.ok(reviewService.byUser(id, page, size));
	}
}
