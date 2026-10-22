package com.campusmarket.user;

import com.campusmarket.common.ApiResponse;
import com.campusmarket.common.PageResult;
import com.campusmarket.auth.CurrentUser;
import com.campusmarket.user.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "通知", description = "站内通知与未读数")
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

	private final NotificationService notificationService;
	private final CurrentUser currentUser;

	public NotificationController(NotificationService notificationService, CurrentUser currentUser) {
		this.notificationService = notificationService;
		this.currentUser = currentUser;
	}

	@Operation(summary = "通知列表")
	@GetMapping
	public ApiResponse<PageResult<NotificationResponse>> list(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size) {
		return ApiResponse.ok(notificationService.list(currentUser.requireId(), page, size));
	}

	@Operation(summary = "未读数量")
	@GetMapping("/unread-count")
	public ApiResponse<Long> unreadCount() {
		return ApiResponse.ok(notificationService.unreadCount(currentUser.requireId()));
	}

	@Operation(summary = "标记单条已读")
	@PostMapping("/{id}/read")
	public ApiResponse<Void> markRead(@PathVariable Long id) {
		notificationService.markRead(currentUser.requireId(), id);
		return ApiResponse.ok();
	}

	@Operation(summary = "全部标记已读")
	@PostMapping("/read-all")
	public ApiResponse<Void> markAllRead() {
		notificationService.markAllRead(currentUser.requireId());
		return ApiResponse.ok();
	}
}
