package com.campusmarket.review;

import com.campusmarket.catalog.Item;
import com.campusmarket.catalog.ItemRepository;
import com.campusmarket.common.BusinessException;
import com.campusmarket.common.ErrorCode;
import com.campusmarket.common.PageResult;
import com.campusmarket.credit.CreditService;
import com.campusmarket.review.dto.CreateReviewRequest;
import com.campusmarket.review.dto.ReviewResponse;
import com.campusmarket.trade.Order;
import com.campusmarket.trade.OrderRepository;
import com.campusmarket.trade.OrderStatus;
import com.campusmarket.user.NotificationService;
import com.campusmarket.user.NotificationType;
import com.campusmarket.user.User;
import com.campusmarket.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {

	private static final int REVIEW_WINDOW_DAYS = 7;

	private final ReviewRepository reviewRepository;
	private final OrderRepository orderRepository;
	private final ItemRepository itemRepository;
	private final UserRepository userRepository;
	private final CreditService creditService;
	private final NotificationService notificationService;

	public ReviewService(ReviewRepository reviewRepository,
			OrderRepository orderRepository,
			ItemRepository itemRepository,
			UserRepository userRepository,
			CreditService creditService,
			NotificationService notificationService) {
		this.reviewRepository = reviewRepository;
		this.orderRepository = orderRepository;
		this.itemRepository = itemRepository;
		this.userRepository = userRepository;
		this.creditService = creditService;
		this.notificationService = notificationService;
	}

	@Transactional
	public ReviewResponse create(Long userId, Long orderId, CreateReviewRequest request) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
		if (!order.isParticipant(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "无权评价该订单");
		}
		if (order.getStatus() != OrderStatus.COMPLETED) {
			throw new BusinessException(ErrorCode.CONFLICT, "订单完成后才能评价");
		}
		if (order.getCompletedAt() != null
				&& order.getCompletedAt().plusDays(REVIEW_WINDOW_DAYS).isBefore(LocalDateTime.now())) {
			throw new BusinessException(ErrorCode.CONFLICT, "已超过 7 天评价期限");
		}
		if (reviewRepository.existsByOrderIdAndReviewerId(orderId, userId)) {
			throw new BusinessException(ErrorCode.CONFLICT, "你已经评价过该订单");
		}

		boolean asBuyer = order.getBuyerId().equals(userId);
		Long revieweeId = asBuyer ? order.getSellerId() : order.getBuyerId();

		Review review = new Review();
		review.setOrderId(orderId);
		review.setReviewerId(userId);
		review.setRevieweeId(revieweeId);
		review.setItemId(order.getItemId());
		review.setDirection(asBuyer ? ReviewDirection.BUYER_TO_SELLER : ReviewDirection.SELLER_TO_BUYER);
		review.setRating(request.rating());
		review.setTags(request.tags());
		review.setComment(request.comment());
		reviewRepository.save(review);

		creditService.recompute(revieweeId);
		notificationService.push(revieweeId, NotificationType.REVIEW, "收到新评价",
				"你收到一条 " + request.rating() + " 星评价", "/users/" + revieweeId + "/profile");
		return ReviewResponse.of(review, itemTitleOf(order.getItemId()), nicknameOf(userId));
	}

	@Transactional(readOnly = true)
	public List<ReviewResponse> byOrder(Long userId, Long orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
		if (!order.isParticipant(userId) && !isModerator(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该订单评价");
		}
		String itemTitle = itemTitleOf(order.getItemId());
		return reviewRepository.findByOrderIdOrderByIdAsc(orderId).stream()
				.map(review -> ReviewResponse.of(review, itemTitle, nicknameOf(review.getReviewerId())))
				.toList();
	}

	@Transactional(readOnly = true)
	public PageResult<ReviewResponse> byUser(Long userId, int page, int size) {
		var result = reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(userId,
				PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 50)));
		return PageResult.of(result,
				review -> ReviewResponse.of(review, itemTitleOf(review.getItemId()), nicknameOf(review.getReviewerId())));
	}

	private boolean isModerator(Long userId) {
		return userRepository.findById(userId).map(User::isModerator).orElse(false);
	}

	private String itemTitleOf(Long itemId) {
		return itemRepository.findById(itemId).map(Item::getTitle).orElse("商品已删除");
	}

	private String nicknameOf(Long userId) {
		return userRepository.findById(userId).map(User::getNickname).orElse("已注销用户");
	}
}
