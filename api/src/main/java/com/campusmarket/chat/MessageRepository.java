package com.campusmarket.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

	List<Message> findByConversationIdAndIdGreaterThanOrderByIdAsc(Long conversationId, Long afterId);

	List<Message> findTop200ByConversationIdOrderByIdDesc(Long conversationId);
}
