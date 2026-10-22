package com.campusmarket.catalog.dto;

import com.campusmarket.catalog.ConditionLevel;
import com.campusmarket.catalog.Item;
import com.campusmarket.catalog.ItemStatus;
import com.campusmarket.catalog.TradeMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ItemDetailResponse(
		Long id,
		String title,
		String description,
		BigDecimal price,
		BigDecimal originalPrice,
		ConditionLevel conditionLevel,
		TradeMethod tradeMethod,
		ItemStatus status,
		String rejectReason,
		String tags,
		String campus,
		Long categoryId,
		String categoryName,
		Long sellerId,
		String sellerNickname,
		String sellerAvatar,
		boolean sellerVerified,
		Integer sellerCreditScore,
		int viewCount,
		int favoriteCount,
		boolean favorited,
		boolean mine,
		List<String> images,
		LocalDateTime publishedAt,
		LocalDateTime createdAt) {

	public static ItemDetailResponse of(Item item, List<String> images, boolean favorited, boolean mine,
			Integer sellerCreditScore) {
		return new ItemDetailResponse(
				item.getId(),
				item.getTitle(),
				item.getDescription(),
				item.getPrice(),
				item.getOriginalPrice(),
				item.getConditionLevel(),
				item.getTradeMethod(),
				item.getStatus(),
				item.getRejectReason(),
				item.getTags(),
				item.getCampus(),
				item.getCategory() == null ? null : item.getCategory().getId(),
				item.getCategory() == null ? null : item.getCategory().getName(),
				item.getSellerId(),
				item.getSeller() == null ? null : item.getSeller().getNickname(),
				item.getSeller() == null ? null : item.getSeller().getAvatarUrl(),
				item.getSeller() != null && item.getSeller().isVerified(),
				sellerCreditScore,
				item.getViewCount(),
				item.getFavoriteCount(),
				favorited,
				mine,
				images,
				item.getPublishedAt(),
				item.getCreatedAt());
	}
}
