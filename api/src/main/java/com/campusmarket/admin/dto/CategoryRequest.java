package com.campusmarket.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
		@NotBlank(message = "分类名称不能为空")
		@Size(max = 40, message = "分类名称最多 40 个字符")
		String name,

		@NotBlank(message = "分类标识不能为空")
		@Size(max = 40, message = "分类标识最多 40 个字符")
		String slug,

		@Size(max = 255, message = "图标地址过长")
		String icon,

		int sortOrder,

		boolean enabled) {
}
