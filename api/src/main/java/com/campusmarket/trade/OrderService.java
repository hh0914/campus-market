package com.campusmarket.trade;

import com.campusmarket.catalog.Item;
import com.campusmarket.catalog.ItemRepository;
import com.campusmarket.catalog.ItemStatus;
import com.campusmarket.common.BusinessException;
import com.campusmarket.common.ErrorCode;
import com.campusmarket.common.PageResult;
import com.campusmarket.config.TradeProperties;
import com.campusmarket.credit.CreditService;
import com.campusmarket.review.ReviewRepository;
import com.campusmarket.trade.dto.OrderDetailResponse;
import com.campusmarket.trade.dto.OrderResponse;
import com.campusmarket.trade.dto.OrderStatusLogResponse;
import com.campusmarket.user.NotificationService;
import com.campusmarket.user.NotificationType;
import com.campusmarket.user.User;
import com.campusmarket.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class OrderService {

	private static final Logger log = LoggerFactory.getLogger(OrderService.class);
	private static final DateTimeFormatter ORDER_NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final OrderRepository orderRepository;
	private final OrderStatusLogRepository statusLogRepository;
	private final ItemRepository itemRepository;
	private final PaymentTransactionRepository paymentTransactionRepository;
	private final UserRepository userRepository;
	private final WalletService walletService;
	private final CreditService creditService;
	private final ReviewRepository reviewRepository;
	private final NotificationService notificationService;
	private final TradeProperties tradeProperties;

	public OrderService(OrderRepository orderRepository,
			OrderStatusLogRepository statusLogRepository,
			ItemRepository itemRepository,
			PaymentTransactionRepository paymentTransactionRepository,
			UserRepository userRepository,
			WalletService walletService,
			CreditService creditService,
			ReviewRepository reviewRepository,
			NotificationService notificationService,
			TradeProperties tradeProperties) {
		this.orderRepository = orderRepository;
		this.statusLogRepository = statusLogRepository;
		this.itemRepository = itemRepository;
		this.paymentTransactionRepository = paymentTransactionRepository;
		this.userRepository = userRepository;
		this.walletService = walletService;
		this.creditService = creditService;
		this.reviewRepository = reviewRepository;
		this.notificationService = notificationService;
		this.tradeProperties = tradeProperties;
	}

	@Transactional
	public OrderResponse create(Long buyerId, Long itemId) {
		User buyer = requireUser(buyerId);
		if (!buyer.isVerified()) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "请先完成校园认证后再下单");
		}
		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "商品不存在"));
		if (item.getSellerId().equals(buyerId)) {
			throw new BusinessException(ErrorCode.BUSINESS_ERROR, "不能购买自己发布的商品");
		}
		if (item.getStatus() != ItemStatus.ON_SALE) {
			throw new BusinessException(ErrorCode.CONFLICT, "商品已被下单或已下架");
		}

		item.setStatus(ItemStatus.LOCKED);
		itemRepository.saveAndFlush(item);

		Order order = new Order();
		order.setOrderNo(generateOrderNo());
		order.setItemId(item.getId());
		order.setBuyerId(buyerId);
		order.setSellerId(item.getSellerId());
		order.setAmount(item.getPrice());
		order.setTradeMethod(item.getTradeMethod());
		order.setStatus(OrderStatus.PENDING_PAY);
		order.setPayDeadline(LocalDateTime.now().plusMinutes(tradeProperties.paymentTimeoutMinutes()));
		orderRepository.save(order);
		statusLogRepository.save(OrderStatusLog.of(order.getId(), OrderStatus.PENDING_PAY, OrderStatus.PENDING_PAY,
				buyerId, "创建订单，商品已锁定"));
		return toResponse(order, buyerId);
	}

	@Transactional
	public OrderResponse pay(Long userId, Long orderId) {
		Order order = requireOrder(orderId);
		if (!order.getBuyerId().equals(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "只有买家可以付款");
		}
		if (order.getStatus() != OrderStatus.PENDING_PAY) {
			throw new BusinessException(ErrorCode.CONFLICT, "订单当前状态不能付款");
		}
		if (order.getPayDeadline().isBefore(LocalDateTime.now())) {
			cancelOrder(order, userId, "超时未支付自动取消");
			throw new BusinessException(ErrorCode.CONFLICT, "订单已超时取消，请重新下单");
		}

		Wallet wallet = walletService.lock(userId);
		walletService.assertEnough(wallet, order.getAmount());
		wallet.setBalance(wallet.getBalance().subtract(order.getAmount()));
		wallet.setFrozen(wallet.getFrozen().add(order.getAmount()));
		wallet.setTotalSpent(wallet.getTotalSpent().add(order.getAmount()));
		walletService.record(wallet, userId, orderId, LedgerType.ESCROW_OUT, order.getAmount().negate(), "托管付款");
		walletService.recordTransaction(orderId, userId, TxType.ESCROW_IN, order.getAmount(), "托管付款");

		order.setPaidAt(LocalDateTime.now());
		transit(order, OrderStatus.PAID, userId, "买家完成托管付款");
		notificationService.push(order.getSellerId(), NotificationType.ORDER, "订单已付款",
				"订单 " + order.getOrderNo() + " 买家已付款，请尽快发货或约定面交", "/orders/" + order.getId());
		return toResponse(order, userId);
	}

	@Transactional
	public OrderResponse cancel(Long userId, Long orderId, String reason) {
		Order order = requireOrder(orderId);
		if (!order.isParticipant(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该订单");
		}
		if (order.getStatus() != OrderStatus.PENDING_PAY) {
			throw new BusinessException(ErrorCode.CONFLICT, "只有待付款订单可以取消");
		}
		cancelOrder(order, userId, reason == null || reason.isBlank() ? "用户取消订单" : reason.trim());
		return toResponse(order, userId);
	}

	@Transactional
	public OrderResponse ship(Long userId, Long orderId) {
		Order order = requireOrder(orderId);
		if (!order.getSellerId().equals(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "只有卖家可以发货");
		}
		order.setShippedAt(LocalDateTime.now());
		transit(order, OrderStatus.SHIPPED, userId, "卖家已发货或已约定面交");
		notificationService.push(order.getBuyerId(), NotificationType.ORDER, "卖家已发货",
				"订单 " + order.getOrderNo() + " 已发货，请及时确认收货", "/orders/" + order.getId());
		return toResponse(order, userId);
	}

	@Transactional
	public OrderResponse confirm(Long userId, Long orderId) {
		Order order = requireOrder(orderId);
		if (!order.getBuyerId().equals(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "只有买家可以确认收货");
		}
		OrderStatusMachine.assertTransition(order.getStatus(), OrderStatus.COMPLETED);

		BigDecimal fee = order.getAmount()
				.multiply(BigDecimal.valueOf(tradeProperties.platformFeeRate()))
				.setScale(2, RoundingMode.HALF_UP);
		BigDecimal income = order.getAmount().subtract(fee);

		Wallet buyerWallet = walletService.lock(order.getBuyerId());
		buyerWallet.setFrozen(buyerWallet.getFrozen().subtract(order.getAmount()));
		walletService.record(buyerWallet, order.getBuyerId(), orderId, LedgerType.ESCROW_IN, order.getAmount(),
				"托管资金已结算给卖家");

		Wallet sellerWallet = walletService.lock(order.getSellerId());
		sellerWallet.setBalance(sellerWallet.getBalance().add(income));
		sellerWallet.setTotalIncome(sellerWallet.getTotalIncome().add(income));
		walletService.record(sellerWallet, order.getSellerId(), orderId, LedgerType.INCOME, income, "订单结算收入");

		payPlatformFee(orderId, fee);

		order.setFeeAmount(fee);
		order.setSellerIncome(income);
		order.setCompletedAt(LocalDateTime.now());
		transit(order, OrderStatus.COMPLETED, userId, "买家确认收货，平台扣除手续费 " + fee + " 元");

		Item item = itemRepository.findById(order.getItemId()).orElse(null);
		if (item != null) {
			item.setStatus(ItemStatus.SOLD);
			item.setSoldAt(LocalDateTime.now());
			itemRepository.save(item);
		}

		creditService.recompute(order.getBuyerId());
		creditService.recompute(order.getSellerId());

		notificationService.push(order.getSellerId(), NotificationType.ORDER, "交易完成",
				"订单 " + order.getOrderNo() + " 已完成，实收 " + income + " 元", "/orders/" + order.getId());
		notificationService.push(order.getBuyerId(), NotificationType.REVIEW, "别忘了评价",
				"订单 " + order.getOrderNo() + " 已完成，去给卖家一个评价吧", "/orders/" + order.getId());
		log.info("订单结算完成 orderNo={} amount={} fee={} income={}", order.getOrderNo(), order.getAmount(), fee, income);
		return toResponse(order, userId);
	}

	private void payPlatformFee(Long orderId, BigDecimal fee) {
		userRepository.findByStudentNo("platform").ifPresent(platform -> {
			Wallet platformWallet = walletService.lock(platform.getId());
			platformWallet.setBalance(platformWallet.getBalance().add(fee));
			walletService.record(platformWallet, platform.getId(), orderId, LedgerType.FEE, fee, "平台交易手续费");
			walletService.recordTransaction(orderId, platform.getId(), TxType.FEE, fee, "平台交易手续费");
		});
	}

	@Transactional
	public OrderResponse refundRequest(Long userId, Long orderId, String reason) {
		Order order = requireOrder(orderId);
		if (!order.getBuyerId().equals(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "只有买家可以申请退款");
		}
		transit(order, OrderStatus.REFUNDING, userId,
				"买家申请退款：" + (reason == null || reason.isBlank() ? "未填写原因" : reason.trim()));
		notificationService.push(order.getSellerId(), NotificationType.ORDER, "买家申请退款",
				"订单 " + order.getOrderNo() + " 买家申请退款，请及时处理", "/orders/" + order.getId());
		return toResponse(order, userId);
	}

	@Transactional
	public OrderResponse refundApprove(Long userId, Long orderId, boolean moderator) {
		Order order = requireOrder(orderId);
		if (!moderator && !order.getSellerId().equals(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "只有卖家或审核员可以处理退款");
		}
		if (order.getStatus() != OrderStatus.REFUNDING) {
			throw new BusinessException(ErrorCode.CONFLICT, "订单不在退款中状态");
		}
		Wallet buyerWallet = walletService.lock(order.getBuyerId());
		buyerWallet.setFrozen(buyerWallet.getFrozen().subtract(order.getAmount()));
		buyerWallet.setBalance(buyerWallet.getBalance().add(order.getAmount()));
		walletService.record(buyerWallet, order.getBuyerId(), orderId, LedgerType.REFUND, order.getAmount(), "退款到账");
		walletService.recordTransaction(orderId, order.getBuyerId(), TxType.REFUND, order.getAmount(), "订单退款");

		order.setCompletedAt(null);
		transit(order, OrderStatus.REFUNDED, userId, "退款已同意，托管资金退回买家");

		Item item = itemRepository.findById(order.getItemId()).orElse(null);
		if (item != null && item.getStatus() == ItemStatus.LOCKED) {
			item.setStatus(ItemStatus.ON_SALE);
			itemRepository.save(item);
		}
		if (order.getShippedAt() != null) {
			creditService.addDispute(order.getSellerId());
		}
		creditService.recompute(order.getBuyerId());
		creditService.recompute(order.getSellerId());
		notificationService.push(order.getBuyerId(), NotificationType.ORDER, "退款已完成",
				"订单 " + order.getOrderNo() + " 的退款已到账", "/orders/" + order.getId());
		return toResponse(order, userId);
	}

	@Transactional
	public OrderResponse refundReject(Long userId, Long orderId, boolean moderator, String reason) {
		Order order = requireOrder(orderId);
		if (!moderator && !order.getSellerId().equals(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "只有卖家或审核员可以处理退款");
		}
		transit(order, OrderStatus.PAID, userId,
				"退款被拒绝：" + (reason == null || reason.isBlank() ? "未填写原因" : reason.trim()));
		notificationService.push(order.getBuyerId(), NotificationType.ORDER, "退款申请被拒绝",
				"订单 " + order.getOrderNo() + " 的退款申请被拒绝", "/orders/" + order.getId());
		return toResponse(order, userId);
	}

	@Transactional(readOnly = true)
	public PageResult<OrderResponse> list(Long userId, String role, String statusFilter, int page, int size) {
		Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 50));
		boolean asSeller = "seller".equalsIgnoreCase(role);
		OrderStatus status = statusFilter == null || statusFilter.isBlank() ? null
				: OrderStatus.valueOf(statusFilter.toUpperCase());
		Page<Order> result;
		if (asSeller) {
			result = status == null
					? orderRepository.findBySellerIdOrderByCreatedAtDesc(userId, pageable)
					: orderRepository.findBySellerIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
		}
		else {
			result = status == null
					? orderRepository.findByBuyerIdOrderByCreatedAtDesc(userId, pageable)
					: orderRepository.findByBuyerIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
		}
		return PageResult.of(result, order -> toResponse(order, userId));
	}

	@Transactional(readOnly = true)
	public OrderDetailResponse detail(Long userId, Long orderId, boolean moderator) {
		Order order = requireOrder(orderId);
		if (!moderator && !order.isParticipant(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该订单");
		}
		List<OrderStatusLogResponse> timeline = statusLogRepository.findByOrderIdOrderByIdAsc(orderId).stream()
				.map(statusLog -> new OrderStatusLogResponse(statusLog.getCreatedAt(),
						statusLog.getFromStatus().name(), statusLog.getToStatus().name(), statusLog.getRemark(),
						nicknameOf(statusLog.getOperatorId())))
				.toList();
		List<OrderDetailResponse.PaymentRecord> payments = paymentTransactionRepository
				.findByOrderIdOrderByIdAsc(orderId).stream()
				.map(transaction -> new OrderDetailResponse.PaymentRecord(transaction.getId(),
						transaction.getTradeNo(), transaction.getType().name(), transaction.getAmount(),
						transaction.getStatus(), transaction.getCreatedAt()))
				.toList();
		boolean reviewed = reviewRepository.existsByOrderIdAndReviewerId(orderId, userId);
		return new OrderDetailResponse(toResponse(order, userId), timeline, reviewed,
				BigDecimal.valueOf(tradeProperties.platformFeeRate()), payments);
	}

	@Transactional
	public int cancelExpiredOrders() {
		List<Order> expired = orderRepository.findTop50ByStatusAndPayDeadlineBefore(OrderStatus.PENDING_PAY,
				LocalDateTime.now());
		int cancelled = 0;
		for (Order order : expired) {
			cancelOrder(order, null, "超时未支付自动取消");
			cancelled++;
		}
		return cancelled;
	}

	private void cancelOrder(Order order, Long operatorId, String reason) {
		OrderStatusMachine.assertTransition(order.getStatus(), OrderStatus.CANCELLED);
		order.setCancelledAt(LocalDateTime.now());
		order.setCancelReason(reason);
		transit(order, OrderStatus.CANCELLED, operatorId, reason);
		Item item = itemRepository.findById(order.getItemId()).orElse(null);
		if (item != null && item.getStatus() == ItemStatus.LOCKED) {
			item.setStatus(ItemStatus.ON_SALE);
			itemRepository.save(item);
		}
		notificationService.push(order.getBuyerId(), NotificationType.ORDER, "订单已取消",
				"订单 " + order.getOrderNo() + " 已取消：" + reason, "/orders/" + order.getId());
	}

	private void transit(Order order, OrderStatus to, Long operatorId, String remark) {
		OrderStatus from = order.getStatus();
		OrderStatusMachine.assertTransition(from, to);
		order.setStatus(to);
		orderRepository.save(order);
		statusLogRepository.save(OrderStatusLog.of(order.getId(), from, to, operatorId, remark));
	}

	private OrderResponse toResponse(Order order, Long viewerId) {
		Item item = itemRepository.findById(order.getItemId()).orElse(null);
		boolean buyer = viewerId != null && viewerId.equals(order.getBuyerId());
		boolean seller = viewerId != null && viewerId.equals(order.getSellerId());
		OrderStatus status = order.getStatus();

		boolean canPay = buyer && status == OrderStatus.PENDING_PAY && order.getPayDeadline().isAfter(LocalDateTime.now());
		boolean canCancel = (buyer || seller) && status == OrderStatus.PENDING_PAY;
		boolean canShip = seller && status == OrderStatus.PAID;
		boolean canConfirm = buyer && status == OrderStatus.SHIPPED;
		boolean canRefundRequest = buyer && (status == OrderStatus.PAID || status == OrderStatus.SHIPPED);
		boolean canRefundHandle = seller && status == OrderStatus.REFUNDING;
		boolean canReview = (buyer || seller) && status == OrderStatus.COMPLETED
				&& viewerId != null
				&& !reviewRepository.existsByOrderIdAndReviewerId(order.getId(), viewerId);

		return new OrderResponse(
				order.getId(),
				order.getOrderNo(),
				order.getItemId(),
				item == null ? null : item.getTitle(),
				item == null ? null : item.getCoverUrl(),
				order.getBuyerId(),
				nicknameOf(order.getBuyerId()),
				order.getSellerId(),
				nicknameOf(order.getSellerId()),
				order.getAmount(),
				order.getFeeAmount(),
				order.getSellerIncome(),
				status,
				TradeMethodView.of(order.getTradeMethod()),
				order.getPayDeadline(),
				order.getPaidAt(),
				order.getShippedAt(),
				order.getCompletedAt(),
				order.getCancelledAt(),
				order.getCancelReason(),
				order.getCreatedAt(),
				new OrderResponse.ActionFlags(canPay, canCancel, canShip, canConfirm, canRefundRequest,
						canRefundHandle, canRefundHandle, canReview));
	}

	private String nicknameOf(Long userId) {
		if (userId == null) {
			return "系统";
		}
		return userRepository.findById(userId).map(User::getNickname).orElse("已注销用户");
	}

	public Order requireOrder(Long orderId) {
		return orderRepository.findById(orderId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
	}

	private User requireUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
	}

	private String generateOrderNo() {
		return "CM" + LocalDateTime.now().format(ORDER_NO_DATE)
				+ String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
	}

	public Map<OrderStatus, Long> countByStatus() {
		return java.util.Arrays.stream(OrderStatus.values())
				.collect(Collectors.toMap(status -> status, orderRepository::countByStatus));
	}
}
