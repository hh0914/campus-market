package com.campusmarket.chat.dto;

import com.campusmarket.chat.Message;
import com.campusmarket.chat.MessageType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MessageResponse(Long id, Long conversationId, Long senderId, String senderNickname, MessageType type,
		String content, BigDecimal offerAmount, LocalDateTime createdAt) {

	public static MessageResponse of(Message message, String senderNickname) {
		return new MessageResponse(message.getId(), message.getConversationId(), message.getSenderId(), senderNickname,
				message.getType(), message.getContent(), message.getOfferAmount(), message.getCreatedAt());
	}
}
