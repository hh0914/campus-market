package com.campusmarket.moderation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HandleReportRequest(
		@NotNull(message = "处理动作不能为空") Action action,
		@NotBlank(message = "处理意见不能为空")
		@Size(min = 3, max = 300, message = "处理意见需 3-300 个字符")
		String remark) {

	public enum Action {
		DOWN_ITEM, WARN, BAN, NONE
	}
}
