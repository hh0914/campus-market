package com.campusmarket.catalog;

import com.campusmarket.auth.CurrentUser;
import com.campusmarket.catalog.dto.FavoriteToggleResponse;
import com.campusmarket.catalog.dto.ItemDetailResponse;
import com.campusmarket.catalog.dto.ItemRequest;
import com.campusmarket.catalog.dto.ItemSummaryResponse;
import com.campusmarket.common.ApiResponse;
import com.campusmarket.common.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "商品", description = "发布、检索、审核流转与收藏")
@RestController
@RequestMapping("/api/v1/items")
public class ItemController {

	private final ItemService itemService;
	private final CurrentUser currentUser;

	public ItemController(ItemService itemService, CurrentUser currentUser) {
		this.itemService = itemService;
		this.currentUser = currentUser;
	}

	@Operation(summary = "商品检索")
	@GetMapping
	public ApiResponse<PageResult<ItemSummaryResponse>> search(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Long categoryId,
			@RequestParam(required = false) ConditionLevel condition,
			@RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice,
			@RequestParam(required = false, defaultValue = "newest") String sort,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "12") int size) {
		Long viewerId = currentUser.find().map(user -> user.id()).orElse(null);
		return ApiResponse.ok(itemService.search(keyword, categoryId, condition, minPrice, maxPrice, sort, page, size,
				viewerId));
	}

	@Operation(summary = "我的商品")
	@GetMapping("/mine")
	public ApiResponse<PageResult<ItemSummaryResponse>> mine(
			@RequestParam(required = false) String status,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size) {
		return ApiResponse.ok(itemService.mine(currentUser.requireId(), status, page, size));
	}

	@Operation(summary = "商品详情")
	@GetMapping("/{id}")
	public ApiResponse<ItemDetailResponse> detail(@PathVariable Long id) {
		return ApiResponse.ok(itemService.detail(id));
	}

	@Operation(summary = "发布商品")
	@PostMapping
	public ApiResponse<ItemDetailResponse> create(@Valid @RequestBody ItemRequest request) {
		return ApiResponse.ok(itemService.create(currentUser.requireId(), request));
	}

	@Operation(summary = "编辑商品")
	@PutMapping("/{id}")
	public ApiResponse<ItemDetailResponse> update(@PathVariable Long id, @Valid @RequestBody ItemRequest request) {
		return ApiResponse.ok(itemService.update(currentUser.requireId(), id, request));
	}

	@Operation(summary = "提交审核")
	@PostMapping("/{id}/submit")
	public ApiResponse<ItemDetailResponse> submit(@PathVariable Long id) {
		return ApiResponse.ok(itemService.submit(currentUser.requireId(), id));
	}

	@Operation(summary = "下架商品")
	@PostMapping("/{id}/off-shelf")
	public ApiResponse<Void> offShelf(@PathVariable Long id) {
		itemService.offShelf(currentUser.requireId(), id);
		return ApiResponse.ok();
	}

	@Operation(summary = "同类推荐")
	@GetMapping("/{id}/related")
	public ApiResponse<List<ItemSummaryResponse>> related(@PathVariable Long id) {
		return ApiResponse.ok(itemService.related(id));
	}

	@Operation(summary = "卖家在售商品")
	@GetMapping("/seller/{sellerId}")
	public ApiResponse<PageResult<ItemSummaryResponse>> bySeller(
			@PathVariable Long sellerId,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "12") int size) {
		return ApiResponse.ok(itemService.bySeller(sellerId, page, size));
	}

	@Operation(summary = "收藏")
	@PostMapping("/{id}/favorite")
	public ApiResponse<FavoriteToggleResponse> favorite(@PathVariable Long id) {
		return ApiResponse.ok(itemService.toggleFavorite(currentUser.requireId(), id));
	}

	@Operation(summary = "取消收藏")
	@DeleteMapping("/{id}/favorite")
	public ApiResponse<FavoriteToggleResponse> unfavorite(@PathVariable Long id) {
		return ApiResponse.ok(itemService.toggleFavorite(currentUser.requireId(), id));
	}
}
