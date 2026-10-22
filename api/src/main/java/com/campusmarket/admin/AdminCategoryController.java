package com.campusmarket.admin;

import com.campusmarket.admin.dto.CategoryRequest;
import com.campusmarket.auth.CurrentUser;
import com.campusmarket.catalog.dto.CategoryResponse;
import com.campusmarket.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "后台-分类管理", description = "分类增删改查")
@RestController
@RequestMapping("/api/v1/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

	private final AdminCategoryService adminCategoryService;
	private final CurrentUser currentUser;

	public AdminCategoryController(AdminCategoryService adminCategoryService, CurrentUser currentUser) {
		this.adminCategoryService = adminCategoryService;
		this.currentUser = currentUser;
	}

	@Operation(summary = "全部分类")
	@GetMapping
	public ApiResponse<List<CategoryResponse>> list() {
		return ApiResponse.ok(adminCategoryService.list());
	}

	@Operation(summary = "新增分类")
	@PostMapping
	public ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
		return ApiResponse.ok(adminCategoryService.create(currentUser.requireId(), request));
	}

	@Operation(summary = "编辑分类")
	@PutMapping("/{id}")
	public ApiResponse<CategoryResponse> update(@PathVariable Long id,
			@Valid @RequestBody CategoryRequest request) {
		return ApiResponse.ok(adminCategoryService.update(currentUser.requireId(), id, request));
	}

	@Operation(summary = "删除分类")
	@DeleteMapping("/{id}")
	public ApiResponse<Void> delete(@PathVariable Long id) {
		adminCategoryService.delete(currentUser.requireId(), id);
		return ApiResponse.ok();
	}
}
