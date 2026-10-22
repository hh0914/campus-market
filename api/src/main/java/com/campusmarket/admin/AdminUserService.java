package com.campusmarket.admin;

import com.campusmarket.admin.dto.AdminUserResponse;
import com.campusmarket.catalog.ItemRepository;
import com.campusmarket.catalog.ItemStatus;
import com.campusmarket.common.BusinessException;
import com.campusmarket.common.ErrorCode;
import com.campusmarket.common.PageResult;
import com.campusmarket.credit.CreditService;
import com.campusmarket.credit.ReputationRepository;
import com.campusmarket.moderation.AuditService;
import com.campusmarket.user.NotificationService;
import com.campusmarket.user.NotificationType;
import com.campusmarket.user.Role;
import com.campusmarket.user.User;
import com.campusmarket.user.UserRepository;
import com.campusmarket.user.UserStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 后台用户治理：检索、警告、封禁、解封与角色调整，所有动作写审计日志。
 */
@Service
public class AdminUserService {

	private final UserRepository userRepository;
	private final ReputationRepository reputationRepository;
	private final ItemRepository itemRepository;
	private final CreditService creditService;
	private final NotificationService notificationService;
	private final AuditService auditService;

	public AdminUserService(UserRepository userRepository,
			ReputationRepository reputationRepository,
			ItemRepository itemRepository,
			CreditService creditService,
			NotificationService notificationService,
			AuditService auditService) {
		this.userRepository = userRepository;
		this.reputationRepository = reputationRepository;
		this.itemRepository = itemRepository;
		this.creditService = creditService;
		this.notificationService = notificationService;
		this.auditService = auditService;
	}

	@Transactional(readOnly = true)
	public PageResult<AdminUserResponse> list(String keyword, String roleFilter, String statusFilter, int page,
			int size) {
		Role role = parseRole(roleFilter);
		UserStatus status = parseStatus(statusFilter);
		Specification<User> specification = (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (keyword != null && !keyword.isBlank()) {
				String like = "%" + keyword.trim().toLowerCase() + "%";
				predicates.add(cb.or(
						cb.like(cb.lower(root.get("nickname")), like),
						cb.like(cb.lower(root.get("studentNo")), like),
						cb.like(cb.lower(root.get("email")), like)));
			}
			if (role != null) {
				predicates.add(cb.equal(root.get("role"), role));
			}
			if (status != null) {
				predicates.add(cb.equal(root.get("status"), status));
			}
			return cb.and(predicates.toArray(new Predicate[0]));
		};
		Page<User> result = userRepository.findAll(specification,
				PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 50),
						Sort.by(Sort.Direction.DESC, "createdAt")));
		return PageResult.of(result, this::toResponse);
	}

	@Transactional
	public AdminUserResponse warn(Long actorId, Long userId, String reason) {
		User user = require(userId);
		assertNotSelf(actorId, userId);
		if (user.getStatus() == UserStatus.BANNED) {
			throw new BusinessException(ErrorCode.CONFLICT, "该用户处于封禁状态，请先解封再警告");
		}
		user.setStatus(UserStatus.WARNED);
		userRepository.save(user);
		notificationService.push(userId, NotificationType.SYSTEM, "收到平台警告", reason, null);
		auditService.log(actorId, "USER_WARN", "USER", userId, "reason=" + reason);
		return toResponse(user);
	}

	@Transactional
	public AdminUserResponse ban(Long actorId, Long userId, String reason, Integer days) {
		User user = require(userId);
		assertNotSelf(actorId, userId);
		if (user.getRole() == Role.ADMIN) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "不能封禁管理员账号");
		}
		int duration = days == null || days < 1 ? 7 : Math.min(days, 365);
		user.setStatus(UserStatus.BANNED);
		user.setBannedUntil(LocalDateTime.now().plusDays(duration));
		userRepository.save(user);
		creditService.recompute(userId);
		notificationService.push(userId, NotificationType.SYSTEM, "账号被限制",
				"你的账号因「" + reason + "」被封禁 " + duration + " 天", null);
		auditService.log(actorId, "USER_BAN", "USER", userId, "days=" + duration + ", reason=" + reason);
		return toResponse(user);
	}

	@Transactional
	public AdminUserResponse unban(Long actorId, Long userId, String reason) {
		User user = require(userId);
		if (user.getStatus() != UserStatus.BANNED) {
			throw new BusinessException(ErrorCode.CONFLICT, "该用户当前未被封禁");
		}
		user.setStatus(UserStatus.ACTIVE);
		user.setBannedUntil(null);
		userRepository.save(user);
		creditService.recompute(userId);
		notificationService.push(userId, NotificationType.SYSTEM, "账号已解除限制",
				"你的账号已恢复正常" + (reason == null || reason.isBlank() ? "" : "：" + reason), null);
		auditService.log(actorId, "USER_UNBAN", "USER", userId, reason == null ? "" : "reason=" + reason);
		return toResponse(user);
	}

	@Transactional
	public AdminUserResponse changeRole(Long actorId, Long userId, Role role) {
		User user = require(userId);
		assertNotSelf(actorId, userId);
		if (user.getRole() == role) {
			return toResponse(user);
		}
		if (user.getRole() == Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
			throw new BusinessException(ErrorCode.CONFLICT, "至少需要保留一名管理员");
		}
		user.setRole(role);
		userRepository.save(user);
		auditService.log(actorId, "USER_ROLE", "USER", userId, "role=" + role);
		return toResponse(user);
	}

	private AdminUserResponse toResponse(User user) {
		var reputation = reputationRepository.findByUserId(user.getId()).orElse(null);
		long onSale = itemRepository.countBySeller_IdAndStatusIn(user.getId(), List.of(ItemStatus.ON_SALE));
		return AdminUserResponse.of(user, reputation, onSale);
	}

	private User require(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
	}

	private void assertNotSelf(Long actorId, Long userId) {
		if (actorId.equals(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "不能对自己执行该操作");
		}
	}

	private Role parseRole(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Role.valueOf(value.toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.INVALID_PARAM, "未知角色: " + value);
		}
	}

	private UserStatus parseStatus(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return UserStatus.valueOf(value.toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.INVALID_PARAM, "未知状态: " + value);
		}
	}
}
