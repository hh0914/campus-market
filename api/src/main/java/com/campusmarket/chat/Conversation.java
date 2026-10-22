package com.campusmarket.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Conversation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "item_id", nullable = false)
	private Long itemId;

	@Column(name = "buyer_id", nullable = false)
	private Long buyerId;

	@Column(name = "seller_id", nullable = false)
	private Long sellerId;

	@Column(name = "last_message_at")
	private LocalDateTime lastMessageAt;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public boolean involves(Long userId) {
		return buyerId.equals(userId) || sellerId.equals(userId);
	}

	public Long counterpartOf(Long userId) {
		return buyerId.equals(userId) ? sellerId : buyerId;
	}
}
