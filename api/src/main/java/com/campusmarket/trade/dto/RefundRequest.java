package com.campusmarket.trade.dto;

import jakarta.validation.constraints.Size;

public record RefundRequest(
		@Size(max = 200, message = "退款原因最多 200 个字符") String reason) {
}
