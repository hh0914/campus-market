package com.campusmarket.trade.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(OrderResponse order, List<OrderStatusLogResponse> timeline, boolean reviewedByMe,
		BigDecimal platformFeeRate, List<PaymentRecord> payments) {

	public record PaymentRecord(Long id, String tradeNo, String type, BigDecimal amount, String status,
			LocalDateTime createdAt) {
	}
}
