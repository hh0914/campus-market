package com.campusmarket.admin.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
		long totalUsers,
		long newUsers7d,
		long onSaleItems,
		long pendingItems,
		long pendingReports,
		long totalOrders,
		long completedOrders,
		BigDecimal gmv,
		BigDecimal gmv7d,
		long todayOrders,
		long todayUsers) {
}
