package com.campusmarket.trade.dto;

import com.campusmarket.trade.LedgerType;
import com.campusmarket.trade.WalletLedger;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LedgerResponse(Long id, LedgerType type, BigDecimal amount, BigDecimal balanceAfter, Long orderId,
		String remark, LocalDateTime createdAt) {

	public static LedgerResponse of(WalletLedger ledger) {
		return new LedgerResponse(ledger.getId(), ledger.getType(), ledger.getAmount(), ledger.getBalanceAfter(),
				ledger.getOrderId(), ledger.getRemark(), ledger.getCreatedAt());
	}
}
