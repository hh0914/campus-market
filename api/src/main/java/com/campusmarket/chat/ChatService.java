package com.campusmarket.chat;

import com.campusmarket.catalog.Item;
import com.campusmarket.catalog.ItemRepository;
import com.campusmarket.chat.dto.ConversationResponse;
import com.campusmarket.chat.dto.MessageResponse;
import com.campusmarket.chat.dto.SendMessageRequest;
import com.campusmarket.common.BusinessException;
import com.campusmarket.common.ErrorCode;
import com.campusmarket.user.User;
import com.campusmarket.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {

	private final ConversationRepository conversationRepository;
	private final MessageRepository messageRepository;
	private final ItemRepository itemRepository;
	private final UserRepository userRepository;

	public ChatService(ConversationRepository conversationRepository,
			MessageRepository messageRepository,
			ItemRepository itemRepository,
			UserRepository userRepository) {
		this.conversationRepository = conversationRepository;
		this.messageRepository = messageRepository;
		this.itemRepository = itemRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public ConversationResponse open(Long userId, Long itemId) {
		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "商品不存在"));
		if (item.getSellerId().equals(userId)) {
			throw new BusinessException(ErrorCode.BUSINESS_ERROR, "不能和自己聊天");
		}
		Conversation conversation = conversationRepository
				.findByItemIdAndBuyerIdAndSellerId(itemId, userId, item.getSellerId())
				.orElseGet(() -> {
					Conversation created = new Conversation();
					created.setItemId(itemId);
					created.setBuyerId(userId);
					created.setSellerId(item.getSellerId());
					return conversationRepository.save(created);
				});
		return toResponse(conversation, userId);
	}

	@Transactional(readOnly = true)
	public List<ConversationResponse> list(Long userId) {
		return conversationRepository.findMine(userId).stream()
				.map(conversation -> toResponse(conversation, userId))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<MessageResponse> messages(Long userId, Long conversationId, Long afterId) {
		Conversation conversation = requireConversation(conversationId, userId);
		List<Message> messages = afterId == null || afterId <= 0
				? new java.util.ArrayList<>(messageRepository.findTop200ByConversationIdOrderByIdDesc(conversationId))
				: messageRepository.findByConversationIdAndIdGreaterThanOrderByIdAsc(conversationId, afterId);
		if (afterId == null || afterId <= 0) {
			java.util.Collections.reverse(messages);
		}
		return messages.stream()
				.map(message -> MessageResponse.of(message, nicknameOf(message.getSenderId())))
				.toList();
	}

	@Transactional
	public MessageResponse send(Long userId, Long conversationId, SendMessageRequest request) {
		Conversation conversation = requireConversation(conversationId, userId);
		MessageType type = request.type() == null ? MessageType.TEXT : request.type();
		if (type == MessageType.OFFER && request.offerAmount() == null) {
			throw new BusinessException(ErrorCode.INVALID_PARAM, "报价消息必须填写金额");
		}
		Message message = new Message();
		message.setConversationId(conversationId);
		message.setSenderId(userId);
		message.setType(type);
		message.setContent(request.content().trim());
		message.setOfferAmount(request.offerAmount());
		message.setReadFlag(false);
		messageRepository.save(message);

		conversation.setLastMessageAt(LocalDateTime.now());
		conversationRepository.save(conversation);
		return MessageResponse.of(message, nicknameOf(userId));
	}

	@Transactional
	public void markRead(Long userId, Long conversationId) {
		requireConversation(conversationId, userId);
		conversationRepository.markRead(conversationId, userId);
	}

	private Conversation requireConversation(Long conversationId, Long userId) {
		Conversation conversation = conversationRepository.findById(conversationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "会话不存在"));
		if (!conversation.involves(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该会话");
		}
		return conversation;
	}

	private ConversationResponse toResponse(Conversation conversation, Long userId) {
		Item item = itemRepository.findById(conversation.getItemId()).orElse(null);
		Long counterpartId = conversation.counterpartOf(userId);
		String lastMessage = messageRepository.findTop200ByConversationIdOrderByIdDesc(conversation.getId()).stream()
				.findFirst()
				.map(Message::getContent)
				.orElse("");
		long unread = conversationRepository.countUnread(conversation.getId(), userId);
		return new ConversationResponse(conversation.getId(), conversation.getItemId(),
				item == null ? "商品已删除" : item.getTitle(), item == null ? null : item.getCoverUrl(),
				counterpartId, nicknameOf(counterpartId), lastMessage, conversation.getLastMessageAt(), unread);
	}

	private String nicknameOf(Long userId) {
		return userRepository.findById(userId).map(User::getNickname).orElse("已注销用户");
	}
}
