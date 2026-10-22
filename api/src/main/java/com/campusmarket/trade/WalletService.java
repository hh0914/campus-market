package com.campusmarket.trade;

import com.campusmarket.common.BusinessException;
import com.campusmarket.common.ErrorCode;
import com.campusmarket.common.PageResult;
import com.campusmarket.trade.dto.LedgerResponse;
import com.campusmarket.trade.dto.WalletResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class WalletService {

	private final WalletRepository walletRepository;
	private final WalletLedgerRepository walletLedgerRepository;
	private final PaymentTransactionRepository paymentTransactionRepository;

	public WalletService(WalletRepository walletRepository,
			WalletLedgerRepository walletLedgerRepository,
			PaymentTransactionRepository paymentTransactionRepository) {
		this.walletRepository = walletRepository;
		this.walletLedgerRepository = walletLedgerRepository;
		this.paymentTransactionRepository = paymentTransactionRepository;
	}

	public Wallet require(Long userId) {
		return walletRepository.findByUserId(userId)
				.orElseGet(() -> walletRepository.save(Wallet.forUser(userId)));
	}

	public Wallet lock(Long userId) {
		return walletRepository.lockByUserId(userId)
				.orElseGet(() -> walletRepository.save(Wallet.forUser(userId)));
	}

	public WalletResponse view(Long userId) {
		Wallet wallet = require(userId);
		return new WalletResponse(wallet.getId(), wallet.getBalance(), wallet.getFrozen(), wallet.getTotalIncome(),
				wallet.getTotalSpent());
	}

	public PageResult<LedgerResponse> ledger(Long userId, int page, int size) {
		var result = walletLedgerRepository.findByUserIdOrderByIdDesc(userId,
				PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 50)));
		return PageResult.of(result, LedgerResponse::of);
	}

	@Transactional
	public WalletResponse recharge(Long userId, BigDecimal amount) {
		Wallet wallet = lock(userId);
		wallet.setBalance(wallet.getBalance().add(amount));
		walletRepository.save(wallet);
		record(wallet, userId, null, LedgerType.RECHARGE, amount, "演示环境充值");
		return view(userId);
	}

	public void record(Wallet wallet, Long userId, Long orderId, LedgerType type, BigDecimal amount, String remark) {
		WalletLedger ledger = new WalletLedger();
		ledger.setWalletId(wallet.getId());
		ledger.setUserId(userId);
		ledger.setOrderId(orderId);
		ledger.setType(type);
		ledger.setAmount(amount);
		ledger.setBalanceAfter(wallet.getBalance());
		ledger.setRemark(remark);
		walletLedgerRepository.save(ledger);
	}

	public void recordTransaction(Long orderId, Long userId, TxType type, BigDecimal amount, String remark) {
		PaymentTransaction transaction = new PaymentTransaction();
		transaction.setTradeNo("TX" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
				+ String.format("%06d", (int) (Math.random() * 1_000_000)));
		transaction.setOrderId(orderId);
		transaction.setUserId(userId);
		transaction.setType(type);
		transaction.setAmount(amount);
		transaction.setStatus("SUCCESS");
		transaction.setRemark(remark);
		paymentTransactionRepository.save(transaction);
	}

	public void assertEnough(Wallet wallet, BigDecimal amount) {
		if (wallet.getBalance().compareTo(amount) < 0) {
			throw new BusinessException(ErrorCode.BUSINESS_ERROR,
					"余额不足，当前可用余额 " + wallet.getBalance() + " 元");
		}
	}
}
