package com.campusmarket.trade.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RechargeRequest(
		@NotNull(message = "充值金额不能为空")
		@DecimalMin(value = "1.00", message = "单次充值至少 1 元")
		@DecimalMax(value = "10000.00", message = "单次充值最多 10000 元")
		BigDecimal amount) {
}
