package com.campusmarket.trade.dto;

import com.campusmarket.trade.OrderStatus;
import com.campusmarket.trade.TradeMethodView;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
		Long id,
		String orderNo,
		Long itemId,
		String itemTitle,
		String itemCover,
		Long buyerId,
		String buyerNickname,
		Long sellerId,
		String sellerNickname,
		BigDecimal amount,
		BigDecimal feeAmount,
		BigDecimal sellerIncome,
		OrderStatus status,
		TradeMethodView tradeMethod,
		LocalDateTime payDeadline,
		LocalDateTime paidAt,
		LocalDateTime shippedAt,
		LocalDateTime completedAt,
		LocalDateTime cancelledAt,
		String cancelReason,
		LocalDateTime createdAt,
		ActionFlags actions) {

	public record ActionFlags(boolean pay, boolean cancel, boolean ship, boolean confirm, boolean refundRequest,
			boolean refundApprove, boolean refundReject, boolean review) {
	}
}
