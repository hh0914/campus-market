package com.campusmarket.user.dto;

import com.campusmarket.credit.Reputation;

import java.math.BigDecimal;

public record ReputationResponse(
		int creditScore,
		String badge,
		String badgeLabel,
		int totalDeals,
		int completedDeals,
		int cancelledDeals,
		int refundedDeals,
		int totalReviews,
		int goodReviews,
		int badReviews,
		int disputeCount,
		int reportCount,
		BigDecimal avgRating) {

	public static ReputationResponse of(Reputation reputation) {
		return new ReputationResponse(
				reputation.getCreditScore(),
				reputation.getBadge().name(),
				reputation.getBadge().getLabel(),
				reputation.getTotalDeals(),
				reputation.getCompletedDeals(),
				reputation.getCancelledDeals(),
				reputation.getRefundedDeals(),
				reputation.getTotalReviews(),
				reputation.getGoodReviews(),
				reputation.getBadReviews(),
				reputation.getDisputeCount(),
				reputation.getReportCount(),
				reputation.getAvgRating());
	}
}
