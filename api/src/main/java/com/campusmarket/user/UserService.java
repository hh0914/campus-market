package com.campusmarket.user;

import com.campusmarket.auth.AuthUser;
import com.campusmarket.auth.CurrentUser;
import com.campusmarket.auth.RefreshTokenRepository;
import com.campusmarket.auth.dto.UserSummary;
import com.campusmarket.catalog.Item;
import com.campusmarket.catalog.ItemRepository;
import com.campusmarket.catalog.ItemStatus;
import com.campusmarket.common.BusinessException;
import com.campusmarket.common.ErrorCode;
import com.campusmarket.credit.Reputation;
import com.campusmarket.credit.ReputationRepository;
import com.campusmarket.review.ReviewRepository;
import com.campusmarket.user.dto.ChangePasswordRequest;
import com.campusmarket.user.dto.PublicProfileResponse;
import com.campusmarket.user.dto.ReputationResponse;
import com.campusmarket.user.dto.UpdateProfileRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final ReputationRepository reputationRepository;
	private final ItemRepository itemRepository;
	private final ReviewRepository reviewRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final CurrentUser currentUser;

	public UserService(UserRepository userRepository,
			ReputationRepository reputationRepository,
			ItemRepository itemRepository,
			ReviewRepository reviewRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			CurrentUser currentUser) {
		this.userRepository = userRepository;
		this.reputationRepository = reputationRepository;
		this.itemRepository = itemRepository;
		this.reviewRepository = reviewRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.currentUser = currentUser;
	}

	public User requireUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
	}

	public PublicProfileResponse publicProfile(Long userId) {
		User user = requireUser(userId);
		Reputation reputation = reputationRepository.findByUserId(userId).orElseGet(() -> Reputation.forUser(userId));
		long onSale = itemRepository.countBySeller_IdAndStatusIn(userId, List.of(ItemStatus.ON_SALE));

		List<PublicProfileResponse.RecentReview> reviews = reviewRepository
				.findByRevieweeIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 5))
				.getContent().stream()
				.map(review -> PublicProfileResponse.RecentReview.of(review,
						userRepository.findById(review.getReviewerId())
								.map(User::getNickname)
								.orElse("匿名用户")))
				.toList();

		return new PublicProfileResponse(
				UserSummary.of(user, reputation.getCreditScore(), reputation.getAvgRating()),
				ReputationResponse.of(reputation),
				onSale,
				user.isVerified(),
				reviews);
	}

	@Transactional
	public UserSummary updateProfile(Long userId, UpdateProfileRequest request) {
		User user = requireUser(userId);
		if (request.nickname() != null && !request.nickname().isBlank()) {
			user.setNickname(request.nickname().trim());
		}
		if (request.avatarUrl() != null) {
			user.setAvatarUrl(request.avatarUrl().isBlank() ? null : request.avatarUrl().trim());
		}
		if (request.bio() != null) {
			user.setBio(request.bio().isBlank() ? null : request.bio().trim());
		}
		if (request.campus() != null) {
			user.setCampus(request.campus().isBlank() ? null : request.campus().trim());
		}
		userRepository.save(user);
		Reputation reputation = reputationRepository.findByUserId(userId).orElse(null);
		return UserSummary.of(user,
				reputation == null ? null : reputation.getCreditScore(),
				reputation == null ? null : reputation.getAvgRating());
	}

	@Transactional
	public void changePassword(Long userId, ChangePasswordRequest request) {
		User user = requireUser(userId);
		if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
			throw new BusinessException(ErrorCode.BUSINESS_ERROR, "原密码不正确");
		}
		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		userRepository.save(user);
		refreshTokenRepository.revokeAllByUserId(userId);
	}

	public UserSummary me() {
		AuthUser authUser = currentUser.require();
		User user = requireUser(authUser.id());
		Reputation reputation = reputationRepository.findByUserId(user.getId()).orElse(null);
		return UserSummary.of(user,
				reputation == null ? null : reputation.getCreditScore(),
				reputation == null ? null : reputation.getAvgRating());
	}

	public Item requireItem(Long itemId) {
		return itemRepository.findById(itemId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "商品不存在"));
	}
}
