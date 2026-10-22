package com.campusmarket.admin.dto;

import java.math.BigDecimal;

public record CategoryStatResponse(Long categoryId, String categoryName, long soldCount, BigDecimal gmv) {
}
