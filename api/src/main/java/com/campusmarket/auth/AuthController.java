package com.campusmarket.auth;

import com.campusmarket.auth.dto.LoginRequest;
import com.campusmarket.auth.dto.RefreshRequest;
import com.campusmarket.auth.dto.RegisterRequest;
import com.campusmarket.auth.dto.TokenResponse;
import com.campusmarket.auth.dto.UserSummary;
import com.campusmarket.auth.dto.VerificationConfirmRequest;
import com.campusmarket.auth.dto.VerificationSendResponse;
import com.campusmarket.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证", description = "注册、登录、令牌与校园认证")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final CurrentUser currentUser;

	public AuthController(AuthService authService, CurrentUser currentUser) {
		this.authService = authService;
		this.currentUser = currentUser;
	}

	@Operation(summary = "注册")
	@PostMapping("/register")
	public ApiResponse<TokenResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
		return ApiResponse.ok(authService.register(request, http.getHeader("User-Agent")));
	}

	@Operation(summary = "登录")
	@PostMapping("/login")
	public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
		return ApiResponse.ok(authService.login(request, http.getHeader("User-Agent")));
	}

	@Operation(summary = "刷新令牌")
	@PostMapping("/refresh")
	public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest http) {
		return ApiResponse.ok(authService.refresh(request.refreshToken(), http.getHeader("User-Agent")));
	}

	@Operation(summary = "退出登录")
	@PostMapping("/logout")
	public ApiResponse<Void> logout() {
		authService.logout(currentUser.requireId());
		return ApiResponse.ok();
	}

	@Operation(summary = "发送校园认证验证码")
	@PostMapping("/verification/send")
	public ApiResponse<VerificationSendResponse> send() {
		return ApiResponse.ok(authService.sendVerification(currentUser.requireId()));
	}

	@Operation(summary = "提交校园认证验证码")
	@PostMapping("/verification/confirm")
	public ApiResponse<UserSummary> confirm(@Valid @RequestBody VerificationConfirmRequest request) {
		return ApiResponse.ok(authService.confirmVerification(currentUser.requireId(), request.code()));
	}

	@Operation(summary = "当前登录用户")
	@GetMapping("/me")
	public ApiResponse<UserSummary> me() {
		return ApiResponse.ok(authService.summary(currentUser.requireId()));
	}
}
