package com.campusmarket.trade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "order_no", nullable = false, unique = true, length = 32)
	private String orderNo;

	@Column(name = "item_id", nullable = false)
	private Long itemId;

	@Column(name = "buyer_id", nullable = false)
	private Long buyerId;

	@Column(name = "seller_id", nullable = false)
	private Long sellerId;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(name = "fee_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal feeAmount = BigDecimal.ZERO;

	@Column(name = "seller_income", nullable = false, precision = 12, scale = 2)
	private BigDecimal sellerIncome = BigDecimal.ZERO;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private OrderStatus status = OrderStatus.PENDING_PAY;

	@Enumerated(EnumType.STRING)
	@Column(name = "trade_method", nullable = false, length = 16)
	private com.campusmarket.catalog.TradeMethod tradeMethod = com.campusmarket.catalog.TradeMethod.MEETUP;

	@Column(name = "pay_deadline", nullable = false)
	private LocalDateTime payDeadline;

	@Column(name = "paid_at")
	private LocalDateTime paidAt;

	@Column(name = "shipped_at")
	private LocalDateTime shippedAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@Column(name = "cancelled_at")
	private LocalDateTime cancelledAt;

	@Column(name = "cancel_reason", length = 200)
	private String cancelReason;

	@Version
	@Column(nullable = false)
	private Long version;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	public boolean isParticipant(Long userId) {
		return buyerId.equals(userId) || sellerId.equals(userId);
	}
}
