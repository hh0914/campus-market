package com.campusmarket.trade;

import com.campusmarket.common.BusinessException;
import com.campusmarket.common.ErrorCode;

import java.util.Map;
import java.util.Set;

/**
 * 订单状态机：所有状态变更必须经过断言，禁止 Service 内直接 setStatus。
 */
public final class OrderStatusMachine {

	private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
			OrderStatus.PENDING_PAY, Set.of(OrderStatus.PAID, OrderStatus.CANCELLED),
			OrderStatus.PAID, Set.of(OrderStatus.SHIPPED, OrderStatus.REFUNDING),
			OrderStatus.SHIPPED, Set.of(OrderStatus.COMPLETED, OrderStatus.REFUNDING),
			OrderStatus.REFUNDING, Set.of(OrderStatus.REFUNDED, OrderStatus.PAID),
			OrderStatus.COMPLETED, Set.of(),
			OrderStatus.CANCELLED, Set.of(),
			OrderStatus.REFUNDED, Set.of());

	private OrderStatusMachine() {
	}

	public static boolean canTransit(OrderStatus from, OrderStatus to) {
		return ALLOWED.getOrDefault(from, Set.of()).contains(to);
	}

	public static void assertTransition(OrderStatus from, OrderStatus to) {
		if (!canTransit(from, to)) {
			throw new BusinessException(ErrorCode.CONFLICT, "订单状态不允许该操作: " + from + " -> " + to);
		}
	}
}
