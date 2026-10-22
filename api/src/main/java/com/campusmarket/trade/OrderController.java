package com.campusmarket.trade;

import com.campusmarket.auth.CurrentUser;
import com.campusmarket.common.ApiResponse;
import com.campusmarket.common.PageResult;
import com.campusmarket.trade.dto.CancelOrderRequest;
import com.campusmarket.trade.dto.CreateOrderRequest;
import com.campusmarket.trade.dto.OrderDetailResponse;
import com.campusmarket.trade.dto.OrderResponse;
import com.campusmarket.trade.dto.RefundRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "订单", description = "下单、托管付款、发货、收货与退款")
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

	private final OrderService orderService;
	private final CurrentUser currentUser;

	public OrderController(OrderService orderService, CurrentUser currentUser) {
		this.orderService = orderService;
		this.currentUser = currentUser;
	}

	@Operation(summary = "创建订单")
	@PostMapping
	public ApiResponse<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
		return ApiResponse.ok(orderService.create(currentUser.requireId(), request.itemId()));
	}

	@Operation(summary = "订单列表")
	@GetMapping
	public ApiResponse<PageResult<OrderResponse>> list(
			@RequestParam(defaultValue = "buyer") String role,
			@RequestParam(required = false) String status,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size) {
		return ApiResponse.ok(orderService.list(currentUser.requireId(), role, status, page, size));
	}

	@Operation(summary = "订单详情")
	@GetMapping("/{id}")
	public ApiResponse<OrderDetailResponse> detail(@PathVariable Long id) {
		var authUser = currentUser.require();
		return ApiResponse.ok(orderService.detail(authUser.id(), id, authUser.isModerator()));
	}

	@Operation(summary = "托管付款")
	@PostMapping("/{id}/pay")
	public ApiResponse<OrderResponse> pay(@PathVariable Long id) {
		return ApiResponse.ok(orderService.pay(currentUser.requireId(), id));
	}

	@Operation(summary = "取消订单")
	@PostMapping("/{id}/cancel")
	public ApiResponse<OrderResponse> cancel(@PathVariable Long id,
			@RequestBody(required = false) @Valid CancelOrderRequest request) {
		return ApiResponse.ok(orderService.cancel(currentUser.requireId(), id,
				request == null ? null : request.reason()));
	}

	@Operation(summary = "卖家发货")
	@PostMapping("/{id}/ship")
	public ApiResponse<OrderResponse> ship(@PathVariable Long id) {
		return ApiResponse.ok(orderService.ship(currentUser.requireId(), id));
	}

	@Operation(summary = "确认收货")
	@PostMapping("/{id}/confirm")
	public ApiResponse<OrderResponse> confirm(@PathVariable Long id) {
		return ApiResponse.ok(orderService.confirm(currentUser.requireId(), id));
	}

	@Operation(summary = "申请退款")
	@PostMapping("/{id}/refund")
	public ApiResponse<OrderResponse> refund(@PathVariable Long id,
			@RequestBody(required = false) @Valid RefundRequest request) {
		return ApiResponse.ok(orderService.refundRequest(currentUser.requireId(), id,
				request == null ? null : request.reason()));
	}

	@Operation(summary = "同意退款")
	@PostMapping("/{id}/refund/approve")
	public ApiResponse<OrderResponse> refundApprove(@PathVariable Long id) {
		var authUser = currentUser.require();
		return ApiResponse.ok(orderService.refundApprove(authUser.id(), id, authUser.isModerator()));
	}

	@Operation(summary = "拒绝退款")
	@PostMapping("/{id}/refund/reject")
	public ApiResponse<OrderResponse> refundReject(@PathVariable Long id,
			@RequestBody(required = false) @Valid RefundRequest request) {
		var authUser = currentUser.require();
		return ApiResponse.ok(orderService.refundReject(authUser.id(), id, authUser.isModerator(),
				request == null ? null : request.reason()));
	}
}
