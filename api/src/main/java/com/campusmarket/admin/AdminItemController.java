package com.campusmarket.admin;

import com.campusmarket.admin.dto.ItemRejectRequest;
import com.campusmarket.auth.CurrentUser;
import com.campusmarket.catalog.ItemService;
import com.campusmarket.catalog.dto.ItemDetailResponse;
import com.campusmarket.catalog.dto.ItemSummaryResponse;
import com.campusmarket.common.ApiResponse;
import com.campusmarket.common.PageResult;
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

@Tag(name = "后台-商品审核", description = "待审核队列与审核结论")
@RestController
@RequestMapping("/api/v1/admin/items")
@PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
public class AdminItemController {

	private final ItemService itemService;
	private final CurrentUser currentUser;

	public AdminItemController(ItemService itemService, CurrentUser currentUser) {
		this.itemService = itemService;
		this.currentUser = currentUser;
	}

	@Operation(summary = "待审核队列")
	@GetMapping("/pending")
	public ApiResponse<PageResult<ItemSummaryResponse>> pending(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size) {
		return ApiResponse.ok(itemService.pending(page, size));
	}

	@Operation(summary = "审核通过")
	@PostMapping("/{id}/approve")
	public ApiResponse<ItemDetailResponse> approve(@PathVariable Long id) {
		itemService.approve(currentUser.requireId(), id);
		return ApiResponse.ok(itemService.detail(id));
	}

	@Operation(summary = "审核驳回")
	@PostMapping("/{id}/reject")
	public ApiResponse<ItemDetailResponse> reject(@PathVariable Long id,
			@Valid @RequestBody ItemRejectRequest request) {
		itemService.reject(currentUser.requireId(), id, request.reason());
		return ApiResponse.ok(itemService.detail(id));
	}
}
