package com.campusmarket.credit;

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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reputation")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Reputation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, unique = true)
	private Long userId;

	@Column(name = "credit_score", nullable = false)
	private int creditScore = 80;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CreditBadge badge = CreditBadge.NORMAL;

	@Column(name = "total_deals", nullable = false)
	private int totalDeals;

	@Column(name = "completed_deals", nullable = false)
	private int completedDeals;

	@Column(name = "cancelled_deals", nullable = false)
	private int cancelledDeals;

	@Column(name = "refunded_deals", nullable = false)
	private int refundedDeals;

	@Column(name = "total_reviews", nullable = false)
	private int totalReviews;

	@Column(name = "good_reviews", nullable = false)
	private int goodReviews;

	@Column(name = "bad_reviews", nullable = false)
	private int badReviews;

	@Column(name = "dispute_count", nullable = false)
	private int disputeCount;

	@Column(name = "report_count", nullable = false)
	private int reportCount;

	@Column(name = "avg_rating", nullable = false, precision = 3, scale = 2)
	private BigDecimal avgRating = BigDecimal.ZERO;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	public static Reputation forUser(Long userId) {
		Reputation reputation = new Reputation();
		reputation.setUserId(userId);
		return reputation;
	}
}
