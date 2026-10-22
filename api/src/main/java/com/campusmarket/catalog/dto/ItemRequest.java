package com.campusmarket.catalog.dto;

import com.campusmarket.catalog.ConditionLevel;
import com.campusmarket.catalog.TradeMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ItemRequest(
		@NotBlank(message = "标题不能为空")
		@Size(max = 80, message = "标题最多 80 个字符")
		String title,

		@NotBlank(message = "描述不能为空")
		@Size(min = 10, max = 2000, message = "描述需 10-2000 个字符")
		String description,

		@NotNull(message = "价格不能为空")
		@DecimalMin(value = "0.01", message = "价格必须大于 0")
		BigDecimal price,

		@DecimalMin(value = "0.00", message = "原价不能为负")
		BigDecimal originalPrice,

		@NotNull(message = "成色不能为空")
		ConditionLevel conditionLevel,

		@NotNull(message = "分类不能为空")
		Long categoryId,

		@Size(max = 50, message = "校区名称过长")
		String campus,

		@NotNull(message = "交易方式不能为空")
		TradeMethod tradeMethod,

		@Size(max = 200, message = "标签最多 200 个字符")
		String tags,

		@Size(max = 9, message = "最多上传 9 张图片")
		List<@Size(max = 255) String> imageUrls,

		boolean submitNow) {
}
