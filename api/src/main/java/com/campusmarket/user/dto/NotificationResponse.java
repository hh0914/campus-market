package com.campusmarket.user.dto;

import com.campusmarket.user.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(Long id, String type, String title, String content, String link, boolean read,
		LocalDateTime createdAt) {

	public static NotificationResponse of(Notification notification) {
		return new NotificationResponse(notification.getId(), notification.getType().name(), notification.getTitle(),
				notification.getContent(), notification.getLink(), notification.isReadFlag(),
				notification.getCreatedAt());
	}
}
