package com.campusmarket.moderation;

import com.campusmarket.auth.CurrentUser;
import com.campusmarket.common.ApiResponse;
import com.campusmarket.common.PageResult;
import com.campusmarket.moderation.dto.CreateReportRequest;
import com.campusmarket.moderation.dto.ReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "举报", description = "用户举报商品或用户")
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

	private final ReportService reportService;
	private final CurrentUser currentUser;

	public ReportController(ReportService reportService, CurrentUser currentUser) {
		this.reportService = reportService;
		this.currentUser = currentUser;
	}

	@Operation(summary = "提交举报")
	@PostMapping
	public ApiResponse<ReportResponse> create(@Valid @RequestBody CreateReportRequest request) {
		return ApiResponse.ok(reportService.create(currentUser.requireId(), request));
	}

	@Operation(summary = "我提交的举报")
	@GetMapping("/mine")
	public ApiResponse<PageResult<ReportResponse>> mine(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size) {
		return ApiResponse.ok(reportService.mine(currentUser.requireId(), page, size));
	}
}
