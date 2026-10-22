package com.campusmarket.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ItemRejectRequest(
		@NotBlank(message = "驳回理由不能为空")
		@Size(min = 5, max = 200, message = "驳回理由需 5-200 个字符")
		String reason) {
}
