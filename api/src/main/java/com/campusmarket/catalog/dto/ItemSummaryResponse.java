package com.campusmarket.catalog.dto;

import com.campusmarket.catalog.ConditionLevel;
import com.campusmarket.catalog.Item;
import com.campusmarket.catalog.ItemStatus;
import com.campusmarket.catalog.TradeMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ItemSummaryResponse(
		Long id,
		String title,
		BigDecimal price,
		BigDecimal originalPrice,
		String coverUrl,
		ConditionLevel conditionLevel,
		TradeMethod tradeMethod,
		ItemStatus status,
		Long categoryId,
		String categoryName,
		Long sellerId,
		String sellerNickname,
		int viewCount,
		int favoriteCount,
		boolean favorited,
		LocalDateTime createdAt) {

	public static ItemSummaryResponse of(Item item, boolean favorited) {
		return new ItemSummaryResponse(
				item.getId(),
				item.getTitle(),
				item.getPrice(),
				item.getOriginalPrice(),
				item.getCoverUrl(),
				item.getConditionLevel(),
				item.getTradeMethod(),
				item.getStatus(),
				item.getCategory() == null ? null : item.getCategory().getId(),
				item.getCategory() == null ? null : item.getCategory().getName(),
				item.getSellerId(),
				item.getSeller() == null ? null : item.getSeller().getNickname(),
				item.getViewCount(),
				item.getFavoriteCount(),
				favorited,
				item.getCreatedAt());
	}
}
