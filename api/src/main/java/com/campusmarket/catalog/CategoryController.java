package com.campusmarket.catalog;

import com.campusmarket.catalog.dto.CategoryResponse;
import com.campusmarket.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "分类", description = "商品分类")
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

	private final CategoryService categoryService;

	public CategoryController(CategoryService categoryService) {
		this.categoryService = categoryService;
	}

	@Operation(summary = "启用中的分类")
	@GetMapping
	public ApiResponse<List<CategoryResponse>> list() {
		return ApiResponse.ok(categoryService.listEnabled());
	}
}
