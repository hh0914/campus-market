package com.campusmarket.user;

import com.campusmarket.auth.CurrentUser;
import com.campusmarket.auth.dto.UserSummary;
import com.campusmarket.common.ApiResponse;
import com.campusmarket.user.dto.ChangePasswordRequest;
import com.campusmarket.user.dto.PublicProfileResponse;
import com.campusmarket.user.dto.UpdateProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户", description = "资料与信用档案")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;
	private final CurrentUser currentUser;

	public UserController(UserService userService, CurrentUser currentUser) {
		this.userService = userService;
		this.currentUser = currentUser;
	}

	@Operation(summary = "公开信用档案")
	@GetMapping("/{id}/profile")
	public ApiResponse<PublicProfileResponse> profile(@PathVariable Long id) {
		return ApiResponse.ok(userService.publicProfile(id));
	}

	@Operation(summary = "我的资料")
	@GetMapping("/me")
	public ApiResponse<UserSummary> me() {
		return ApiResponse.ok(userService.me());
	}

	@Operation(summary = "修改资料")
	@PutMapping("/me")
	public ApiResponse<UserSummary> update(@Valid @RequestBody UpdateProfileRequest request) {
		return ApiResponse.ok(userService.updateProfile(currentUser.requireId(), request));
	}

	@Operation(summary = "修改密码")
	@PutMapping("/me/password")
	public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
		userService.changePassword(currentUser.requireId(), request);
		return ApiResponse.ok();
	}
}
