package com.campusmarket.admin;

import com.campusmarket.admin.dto.AdminUserResponse;
import com.campusmarket.admin.dto.ModerationActionRequest;
import com.campusmarket.admin.dto.RoleChangeRequest;
import com.campusmarket.auth.CurrentUser;
import com.campusmarket.common.ApiResponse;
import com.campusmarket.common.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-用户管理", description = "检索、警告、封禁与角色调整")
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
public class AdminUserController {

	private final AdminUserService adminUserService;
	private final CurrentUser currentUser;

	public AdminUserController(AdminUserService adminUserService, CurrentUser currentUser) {
		this.adminUserService = adminUserService;
		this.currentUser = currentUser;
	}

	@Operation(summary = "用户检索")
	@GetMapping
	public ApiResponse<PageResult<AdminUserResponse>> list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String role,
			@RequestParam(required = false) String status,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size) {
		return ApiResponse.ok(adminUserService.list(keyword, role, status, page, size));
	}

	@Operation(summary = "警告用户")
	@PostMapping("/{id}/warn")
	public ApiResponse<AdminUserResponse> warn(@PathVariable Long id,
			@Valid @RequestBody ModerationActionRequest request) {
		return ApiResponse.ok(adminUserService.warn(currentUser.requireId(), id, request.reason()));
	}

	@Operation(summary = "封禁用户")
	@PostMapping("/{id}/ban")
	public ApiResponse<AdminUserResponse> ban(@PathVariable Long id,
			@Valid @RequestBody ModerationActionRequest request) {
		return ApiResponse.ok(
				adminUserService.ban(currentUser.requireId(), id, request.reason(), request.days()));
	}

	@Operation(summary = "解除封禁")
	@PostMapping("/{id}/unban")
	public ApiResponse<AdminUserResponse> unban(@PathVariable Long id,
			@RequestBody(required = false) ModerationActionRequest request) {
		return ApiResponse.ok(adminUserService.unban(currentUser.requireId(), id,
				request == null ? null : request.reason()));
	}

	@Operation(summary = "调整角色")
	@PutMapping("/{id}/role")
	public ApiResponse<AdminUserResponse> changeRole(@PathVariable Long id,
			@Valid @RequestBody RoleChangeRequest request) {
		return ApiResponse.ok(adminUserService.changeRole(currentUser.requireId(), id, request.role()));
	}
}
