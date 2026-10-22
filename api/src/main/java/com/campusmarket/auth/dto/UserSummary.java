package com.campusmarket.auth.dto;

import com.campusmarket.user.User;

import java.math.BigDecimal;

public record UserSummary(
		Long id,
		String studentNo,
		String nickname,
		String email,
		String avatarUrl,
		String campus,
		String role,
		boolean verified,
		Integer creditScore,
		BigDecimal avgRating) {

	public static UserSummary of(User user, Integer creditScore, BigDecimal avgRating) {
		return new UserSummary(
				user.getId(),
				user.getStudentNo(),
				user.getNickname(),
				user.getEmail(),
				user.getAvatarUrl(),
				user.getCampus(),
				user.getRole().name(),
				user.isVerified(),
				creditScore,
				avgRating);
	}
}
