package com.campusmarket.chat.dto;

import com.campusmarket.chat.MessageType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SendMessageRequest(
		@NotBlank(message = "消息内容不能为空")
		@Size(max = 1000, message = "消息最多 1000 个字符")
		String content,

		MessageType type,

		@DecimalMin(value = "0.01", message = "报价必须大于 0")
		BigDecimal offerAmount) {
}
