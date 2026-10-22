package com.campusmarket.auth.dto;

public record TokenResponse(String accessToken, String refreshToken, long expiresIn, UserSummary user) {
}
