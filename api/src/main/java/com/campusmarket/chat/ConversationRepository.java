package com.campusmarket.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

	Optional<Conversation> findByItemIdAndBuyerIdAndSellerId(Long itemId, Long buyerId, Long sellerId);

	@Query("select c from Conversation c where c.buyerId = :userId or c.sellerId = :userId "
			+ "order by coalesce(c.lastMessageAt, c.createdAt) desc")
	List<Conversation> findMine(@Param("userId") Long userId);

	@Query("select count(m) from Message m where m.conversationId = :conversationId "
			+ "and m.senderId <> :userId and m.readFlag = false")
	long countUnread(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

	@Modifying
	@Query("update Message m set m.readFlag = true where m.conversationId = :conversationId and m.senderId <> :userId and m.readFlag = false")
	int markRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}
