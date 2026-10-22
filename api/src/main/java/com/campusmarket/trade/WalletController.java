package com.campusmarket.trade;

import com.campusmarket.common.ApiResponse;
import com.campusmarket.auth.CurrentUser;
import com.campusmarket.common.PageResult;
import com.campusmarket.trade.dto.LedgerResponse;
import com.campusmarket.trade.dto.RechargeRequest;
import com.campusmarket.trade.dto.WalletResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "钱包", description = "余额、冻结与流水")
@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

	private final WalletService walletService;
	private final CurrentUser currentUser;

	public WalletController(WalletService walletService, CurrentUser currentUser) {
		this.walletService = walletService;
		this.currentUser = currentUser;
	}

	@Operation(summary = "我的钱包")
	@GetMapping
	public ApiResponse<WalletResponse> view() {
		return ApiResponse.ok(walletService.view(currentUser.requireId()));
	}

	@Operation(summary = "钱包流水")
	@GetMapping("/ledger")
	public ApiResponse<PageResult<LedgerResponse>> ledger(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size) {
		return ApiResponse.ok(walletService.ledger(currentUser.requireId(), page, size));
	}

	@Operation(summary = "演示充值")
	@PostMapping("/recharge")
	public ApiResponse<WalletResponse> recharge(@Valid @RequestBody RechargeRequest request) {
		return ApiResponse.ok(walletService.recharge(currentUser.requireId(), request.amount()));
	}
}
