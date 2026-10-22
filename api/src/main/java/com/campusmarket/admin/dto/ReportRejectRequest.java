package com.campusmarket.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportRejectRequest(
		@NotBlank(message = "驳回意见不能为空")
		@Size(min = 3, max = 300, message = "驳回意见需 3-300 个字符")
		String remark) {
}
