package com.campusmarket.catalog;

import com.campusmarket.auth.CurrentUser;
import com.campusmarket.catalog.dto.ItemSummaryResponse;
import com.campusmarket.common.ApiResponse;
import com.campusmarket.common.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "收藏", description = "我的收藏")
@RestController
@RequestMapping("/api/v1/favorites")
public class FavoriteController {

	private final ItemService itemService;
	private final CurrentUser currentUser;

	public FavoriteController(ItemService itemService, CurrentUser currentUser) {
		this.itemService = itemService;
		this.currentUser = currentUser;
	}

	@Operation(summary = "我的收藏")
	@GetMapping
	public ApiResponse<PageResult<ItemSummaryResponse>> list(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "12") int size) {
		return ApiResponse.ok(itemService.myFavorites(currentUser.requireId(), page, size));
	}
}
