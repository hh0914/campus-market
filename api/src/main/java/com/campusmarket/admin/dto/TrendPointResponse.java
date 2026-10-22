package com.campusmarket.admin.dto;

import java.math.BigDecimal;

public record TrendPointResponse(String date, long orders, BigDecimal gmv, long newUsers, long newItems) {
}
