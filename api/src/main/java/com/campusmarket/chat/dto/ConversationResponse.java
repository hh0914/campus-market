package com.campusmarket.chat.dto;

import java.time.LocalDateTime;

public record ConversationResponse(Long id, Long itemId, String itemTitle, String itemCover, Long counterpartId,
		String counterpartNickname, String lastMessage, LocalDateTime lastMessageAt, long unreadCount) {
}
