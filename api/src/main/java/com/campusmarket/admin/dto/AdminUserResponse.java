package com.campusmarket.admin.dto;

import com.campusmarket.credit.Reputation;
import com.campusmarket.user.Role;
import com.campusmarket.user.User;
import com.campusmarket.user.UserStatus;

import java.time.LocalDateTime;

/**
 * 后台用户列表行，包含运营需要看到的状态与信用信息。
 */
public record AdminUserResponse(
		Long id,
		String studentNo,
		String nickname,
		String email,
		String avatarUrl,
		String campus,
		Role role,
		UserStatus status,
		boolean verified,
		Integer creditScore,
		String badge,
		long onSaleItems,
		LocalDateTime bannedUntil,
		LocalDateTime lastLoginAt,
		LocalDateTime createdAt) {

	public static AdminUserResponse of(User user, Reputation reputation, long onSaleItems) {
		return new AdminUserResponse(
				user.getId(),
				user.getStudentNo(),
				user.getNickname(),
				user.getEmail(),
				user.getAvatarUrl(),
				user.getCampus(),
				user.getRole(),
				user.getStatus(),
				user.isVerified(),
				reputation == null ? null : reputation.getCreditScore(),
				reputation == null ? null : reputation.getBadge().name(),
				onSaleItems,
				user.getBannedUntil(),
				user.getLastLoginAt(),
				user.getCreatedAt());
	}
}
