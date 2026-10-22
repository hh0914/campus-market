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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_status_log")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class OrderStatusLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "order_id", nullable = false)
	private Long orderId;

	@Enumerated(EnumType.STRING)
	@Column(name = "from_status", nullable = false, length = 16)
	private OrderStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status", nullable = false, length = 16)
	private OrderStatus toStatus;

	@Column(name = "operator_id")
	private Long operatorId;

	@Column(length = 200)
	private String remark;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public static OrderStatusLog of(Long orderId, OrderStatus from, OrderStatus to, Long operatorId, String remark) {
		OrderStatusLog log = new OrderStatusLog();
		log.setOrderId(orderId);
		log.setFromStatus(from);
		log.setToStatus(to);
		log.setOperatorId(operatorId);
		log.setRemark(remark);
		return log;
	}
}
