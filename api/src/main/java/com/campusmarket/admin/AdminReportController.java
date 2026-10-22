package com.campusmarket.admin;

import com.campusmarket.admin.dto.ReportRejectRequest;
import com.campusmarket.auth.CurrentUser;
import com.campusmarket.common.ApiResponse;
import com.campusmarket.common.PageResult;
import com.campusmarket.moderation.ReportService;
import com.campusmarket.moderation.dto.HandleReportRequest;
import com.campusmarket.moderation.dto.ReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-举报工单", description = "工单队列、领取与裁定")
@RestController
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
public class AdminReportController {

	private final ReportService reportService;
	private final CurrentUser currentUser;

	public AdminReportController(ReportService reportService, CurrentUser currentUser) {
		this.reportService = reportService;
		this.currentUser = currentUser;
	}

	@Operation(summary = "工单列表")
	@GetMapping
	public ApiResponse<PageResult<ReportResponse>> list(
			@RequestParam(required = false) String status,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size) {
		return ApiResponse.ok(reportService.page(status, page, size));
	}

	@Operation(summary = "领取工单")
	@PostMapping("/{id}/claim")
	public ApiResponse<ReportResponse> claim(@PathVariable Long id) {
		return ApiResponse.ok(reportService.claim(currentUser.requireId(), id));
	}

	@Operation(summary = "裁定举报成立")
	@PostMapping("/{id}/resolve")
	public ApiResponse<ReportResponse> resolve(@PathVariable Long id,
			@Valid @RequestBody HandleReportRequest request) {
		return ApiResponse.ok(reportService.resolve(currentUser.requireId(), id, request));
	}

	@Operation(summary = "驳回举报")
	@PostMapping("/{id}/reject")
	public ApiResponse<ReportResponse> reject(@PathVariable Long id,
			@Valid @RequestBody ReportRejectRequest request) {
		return ApiResponse.ok(reportService.reject(currentUser.requireId(), id, request.remark()));
	}
}
