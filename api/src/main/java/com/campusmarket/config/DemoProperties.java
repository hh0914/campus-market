package com.campusmarket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 演示开关。生产部署时把 app.demo.expose-verification-code 设为 false。
 */
@ConfigurationProperties(prefix = "app.demo")
public record DemoProperties(boolean exposeVerificationCode) {
}
