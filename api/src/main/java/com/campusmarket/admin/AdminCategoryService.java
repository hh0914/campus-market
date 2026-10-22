package com.campusmarket.admin;

import com.campusmarket.admin.dto.CategoryRequest;
import com.campusmarket.catalog.Category;
import com.campusmarket.catalog.CategoryRepository;
import com.campusmarket.catalog.ItemRepository;
import com.campusmarket.catalog.dto.CategoryResponse;
import com.campusmarket.common.BusinessException;
import com.campusmarket.common.ErrorCode;
import com.campusmarket.moderation.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 分类后台维护。删除前校验是否已有商品引用，避免外键异常直接暴露给前端。
 */
@Service
public class AdminCategoryService {

	private final CategoryRepository categoryRepository;
	private final ItemRepository itemRepository;
	private final AuditService auditService;

	public AdminCategoryService(CategoryRepository categoryRepository, ItemRepository itemRepository,
			AuditService auditService) {
		this.categoryRepository = categoryRepository;
		this.itemRepository = itemRepository;
		this.auditService = auditService;
	}

	@Transactional(readOnly = true)
	public List<CategoryResponse> list() {
		return categoryRepository.findAllByOrderBySortOrderAsc().stream().map(CategoryResponse::of).toList();
	}

	@Transactional
	public CategoryResponse create(Long actorId, CategoryRequest request) {
		String slug = request.slug().trim().toLowerCase();
		if (categoryRepository.existsBySlug(slug)) {
			throw new BusinessException(ErrorCode.CONFLICT, "分类标识已存在: " + slug);
		}
		Category category = new Category();
		category.setName(request.name().trim());
		category.setSlug(slug);
		category.setIcon(blankToNull(request.icon()));
		category.setSortOrder(request.sortOrder());
		category.setEnabled(request.enabled());
		categoryRepository.save(category);
		auditService.log(actorId, "CATEGORY_CREATE", "CATEGORY", category.getId(), "slug=" + slug);
		return CategoryResponse.of(category);
	}

	@Transactional
	public CategoryResponse update(Long actorId, Long id, CategoryRequest request) {
		Category category = require(id);
		String slug = request.slug().trim().toLowerCase();
		if (!category.getSlug().equals(slug) && categoryRepository.existsBySlug(slug)) {
			throw new BusinessException(ErrorCode.CONFLICT, "分类标识已存在: " + slug);
		}
		category.setName(request.name().trim());
		category.setSlug(slug);
		category.setIcon(blankToNull(request.icon()));
		category.setSortOrder(request.sortOrder());
		category.setEnabled(request.enabled());
		categoryRepository.save(category);
		auditService.log(actorId, "CATEGORY_UPDATE", "CATEGORY", id, "slug=" + slug);
		return CategoryResponse.of(category);
	}

	@Transactional
	public void delete(Long actorId, Long id) {
		Category category = require(id);
		long used = itemRepository.countByCategoryId(id);
		if (used > 0) {
			throw new BusinessException(ErrorCode.CONFLICT, "该分类下还有 " + used + " 件商品，无法删除");
		}
		categoryRepository.delete(category);
		auditService.log(actorId, "CATEGORY_DELETE", "CATEGORY", id, "slug=" + category.getSlug());
	}

	private Category require(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "分类不存在"));
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
