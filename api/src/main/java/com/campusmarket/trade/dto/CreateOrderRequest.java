package com.campusmarket.trade.dto;

import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(@NotNull(message = "商品不能为空") Long itemId) {
}
