package com.campusmarket.auth.dto;

/**
 * code 仅在演示模式（app.demo.expose-verification-code=true）下返回，生产环境为 null。
 */
public record VerificationSendResponse(String email, long expiresInSeconds, String hint, String code) {
}
