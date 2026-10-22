package com.campusmarket.auth;

import com.campusmarket.common.BusinessException;
import com.campusmarket.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 便捷获取当前登录用户，并在需要时强制要求认证/审核权限。
 */
@Component
public class CurrentUser {

	public Optional<AuthUser> find() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser authUser)) {
			return Optional.empty();
		}
		return Optional.of(authUser);
	}

	public AuthUser require() {
		return find().orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录"));
	}

	public Long requireId() {
		return require().id();
	}

	public AuthUser requireVerified() {
		AuthUser user = require();
		if (!user.verified()) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "请先完成校园认证");
		}
		return user;
	}

	public void requireOwner(Long ownerId) {
		if (!requireId().equals(ownerId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作他人数据");
		}
	}
}
