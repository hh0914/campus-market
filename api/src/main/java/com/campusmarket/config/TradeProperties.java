package com.campusmarket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.trade")
public record TradeProperties(double platformFeeRate, int paymentTimeoutMinutes) {
}
