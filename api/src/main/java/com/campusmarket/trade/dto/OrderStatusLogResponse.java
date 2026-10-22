package com.campusmarket.trade.dto;

import java.time.LocalDateTime;

public record OrderStatusLogResponse(LocalDateTime createdAt, String fromStatus, String toStatus, String remark,
		String operatorNickname) {
}
