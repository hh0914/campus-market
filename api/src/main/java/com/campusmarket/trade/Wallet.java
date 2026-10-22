package com.campusmarket.trade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
@Table(name = "wallet")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Wallet {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, unique = true)
	private Long userId;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal balance = BigDecimal.ZERO;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal frozen = BigDecimal.ZERO;

	@Column(name = "total_income", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalIncome = BigDecimal.ZERO;

	@Column(name = "total_spent", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalSpent = BigDecimal.ZERO;

	@Version
	@Column(nullable = false)
	private Long version;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	public static Wallet forUser(Long userId) {
		Wallet wallet = new Wallet();
		wallet.setUserId(userId);
		return wallet;
	}
}
