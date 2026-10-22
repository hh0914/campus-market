package com.campusmarket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.credit")
public record CreditProperties(int baseScore, int minPublishThreshold, int unlimitedPublishThreshold) {
}
