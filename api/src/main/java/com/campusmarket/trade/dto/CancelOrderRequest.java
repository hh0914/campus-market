package com.campusmarket.trade.dto;

import jakarta.validation.constraints.Size;

public record CancelOrderRequest(@Size(max = 200, message = "取消原因最多 200 个字符") String reason) {
}
