package com.campusmarket.catalog.dto;

import com.campusmarket.catalog.Category;

public record CategoryResponse(Long id, String name, String slug, String icon, int sortOrder, boolean enabled) {

	public static CategoryResponse of(Category category) {
		return new CategoryResponse(category.getId(), category.getName(), category.getSlug(), category.getIcon(),
				category.getSortOrder(), category.isEnabled());
	}
}
