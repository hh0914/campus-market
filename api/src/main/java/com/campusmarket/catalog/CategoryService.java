package com.campusmarket.catalog;

import com.campusmarket.catalog.dto.CategoryResponse;
import com.campusmarket.common.BusinessException;
import com.campusmarket.common.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

	private final CategoryRepository categoryRepository;

	public CategoryService(CategoryRepository categoryRepository) {
		this.categoryRepository = categoryRepository;
	}

	public List<CategoryResponse> listEnabled() {
		return categoryRepository.findByEnabledTrueOrderBySortOrderAsc().stream()
				.map(CategoryResponse::of)
				.toList();
	}

	public List<CategoryResponse> listAll() {
		return categoryRepository.findAllByOrderBySortOrderAsc().stream()
				.map(CategoryResponse::of)
				.toList();
	}

	public Category require(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "分类不存在"));
	}
}
