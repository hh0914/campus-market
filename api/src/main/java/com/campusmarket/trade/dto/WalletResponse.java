package com.campusmarket.trade.dto;

import java.math.BigDecimal;

public record WalletResponse(Long id, BigDecimal balance, BigDecimal frozen, BigDecimal totalIncome,
		BigDecimal totalSpent) {
}
