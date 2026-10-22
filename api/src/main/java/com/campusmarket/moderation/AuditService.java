package com.campusmarket.moderation;

import com.campusmarket.user.User;
import com.campusmarket.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
public class AuditService {

	private final AuditLogRepository auditLogRepository;
	private final UserRepository userRepository;

	public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
		this.auditLogRepository = auditLogRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public void log(Long actorId, String action, String targetType, Long targetId, String detail) {
		AuditLog log = new AuditLog();
		log.setActorId(actorId);
		log.setAction(action);
		log.setTargetType(targetType);
		log.setTargetId(targetId);
		log.setDetail(detail);
		log.setIp(clientIp());
		auditLogRepository.save(log);
	}

	public Page<AuditView> page(int page, int size) {
		Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 50));
		return auditLogRepository.findAllByOrderByIdDesc(pageable)
				.map(entity -> new AuditView(entity.getId(), entity.getActorId(),
						entity.getActorId() == null ? "系统"
								: userRepository.findById(entity.getActorId()).map(User::getNickname).orElse("未知"),
						entity.getAction(), entity.getTargetType(), entity.getTargetId(), entity.getDetail(),
						entity.getIp(), entity.getCreatedAt()));
	}

	private String clientIp() {
		var attributes = RequestContextHolder.getRequestAttributes();
		if (attributes instanceof ServletRequestAttributes servletAttributes) {
			HttpServletRequest request = servletAttributes.getRequest();
			String forwarded = request.getHeader("X-Forwarded-For");
			if (forwarded != null && !forwarded.isBlank()) {
				return forwarded.split(",")[0].trim();
			}
			return request.getRemoteAddr();
		}
		return null;
	}

	public record AuditView(Long id, Long actorId, String actorNickname, String action, String targetType,
			Long targetId, String detail, String ip, LocalDateTime createdAt) {
	}
}
