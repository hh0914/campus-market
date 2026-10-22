package com.campusmarket.moderation.dto;

import com.campusmarket.moderation.ReportReason;
import com.campusmarket.moderation.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
		@NotNull(message = "举报类型不能为空") ReportTargetType targetType,
		@NotNull(message = "举报对象不能为空") Long targetId,
		@NotNull(message = "举报原因不能为空") ReportReason reasonType,
		@NotBlank(message = "请填写举报描述")
		@Size(min = 5, max = 500, message = "举报描述需 5-500 个字符")
		String description,
		@Size(max = 255) String evidenceUrl) {
}
