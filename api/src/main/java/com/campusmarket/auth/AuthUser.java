package com.campusmarket.auth;

import com.campusmarket.user.Role;

/**
 * 认证后放入 SecurityContext 的主体。
 */
public record AuthUser(Long id, Role role, boolean verified) {

	public boolean isModerator() {
		return role == Role.MODERATOR || role == Role.ADMIN;
	}
}
