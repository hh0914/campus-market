package com.campusmarket.admin;

import com.campusmarket.admin.dto.CategoryStatResponse;
import com.campusmarket.admin.dto.DashboardSummaryResponse;
import com.campusmarket.admin.dto.TrendPointResponse;
import com.campusmarket.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "后台-数据看板", description = "概览指标、趋势与分类分布")
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
public class AdminDashboardController {

	private final AdminDashboardService dashboardService;

	public AdminDashboardController(AdminDashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@Operation(summary = "概览指标")
	@GetMapping("/summary")
	public ApiResponse<DashboardSummaryResponse> summary() {
		return ApiResponse.ok(dashboardService.summary());
	}

	@Operation(summary = "近 N 日趋势")
	@GetMapping("/trend")
	public ApiResponse<List<TrendPointResponse>> trend(@RequestParam(defaultValue = "14") int days) {
		return ApiResponse.ok(dashboardService.trend(days));
	}

	@Operation(summary = "成交分类分布")
	@GetMapping("/categories")
	public ApiResponse<List<CategoryStatResponse>> categories() {
		return ApiResponse.ok(dashboardService.categoryStats());
	}
}
