package com.campusmarket.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModerationActionRequest(
		@NotBlank(message = "处理原因不能为空")
		@Size(min = 2, max = 200, message = "处理原因需 2-200 个字符")
		String reason,

		@Min(value = 1, message = "封禁天数至少 1 天")
		@Max(value = 365, message = "封禁天数最多 365 天")
		Integer days) {
}
