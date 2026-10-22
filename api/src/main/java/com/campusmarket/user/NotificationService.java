package com.campusmarket.user;

import com.campusmarket.common.BusinessException;
import com.campusmarket.common.ErrorCode;
import com.campusmarket.common.PageResult;
import com.campusmarket.user.dto.NotificationResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

	private final NotificationRepository notificationRepository;

	public NotificationService(NotificationRepository notificationRepository) {
		this.notificationRepository = notificationRepository;
	}

	@Transactional
	public void push(Long userId, NotificationType type, String title, String content, String link) {
		notificationRepository.save(Notification.of(userId, type, title, content, link));
	}

	public PageResult<NotificationResponse> list(Long userId, int page, int size) {
		var result = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId,
				PageRequest.of(Math.max(page - 1, 0), clamp(size)));
		return PageResult.of(result, NotificationResponse::of);
	}

	public long unreadCount(Long userId) {
		return notificationRepository.countByUserIdAndReadFlagFalse(userId);
	}

	@Transactional
	public void markRead(Long userId, Long notificationId) {
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "通知不存在"));
		if (!notification.getUserId().equals(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该通知");
		}
		notification.setReadFlag(true);
		notificationRepository.save(notification);
	}

	@Transactional
	public void markAllRead(Long userId) {
		notificationRepository.markAllRead(userId);
	}

	private int clamp(int size) {
		if (size <= 0) {
			return 10;
		}
		return Math.min(size, 50);
	}

	public List<NotificationResponse> latest(Long userId) {
		return notificationRepository
				.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 5))
				.map(NotificationResponse::of)
				.getContent();
	}
}
