package com.campusmarket.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "student_no", nullable = false, unique = true, length = 24)
	private String studentNo;

	@Column(nullable = false, unique = true, length = 120)
	private String email;

	@Column(nullable = false, length = 32)
	private String nickname;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(name = "avatar_url", length = 255)
	private String avatarUrl;

	@Column(length = 200)
	private String bio;

	@Column(length = 50)
	private String campus;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private Role role = Role.STUDENT;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private UserStatus status = UserStatus.ACTIVE;

	@Column(nullable = false)
	private boolean verified;

	@Column(name = "banned_until")
	private LocalDateTime bannedUntil;

	@Column(name = "last_login_at")
	private LocalDateTime lastLoginAt;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	public boolean isActive() {
		if (status != UserStatus.BANNED) {
			return true;
		}
		return bannedUntil != null && bannedUntil.isBefore(LocalDateTime.now());
	}

	public boolean isModerator() {
		return role == Role.MODERATOR || role == Role.ADMIN;
	}
}
