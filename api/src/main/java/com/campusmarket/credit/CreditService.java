package com.campusmarket.credit;

import com.campusmarket.catalog.ItemRepository;
import com.campusmarket.catalog.ItemStatus;
import com.campusmarket.common.BusinessException;
import com.campusmarket.common.ErrorCode;
import com.campusmarket.config.CreditProperties;
import com.campusmarket.review.Review;
import com.campusmarket.review.ReviewRepository;
import com.campusmarket.trade.OrderRepository;
import com.campusmarket.trade.OrderStatus;
import com.campusmarket.user.User;
import com.campusmarket.user.UserRepository;
import com.campusmarket.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 信用分：由评价、履约率与扣分项实时重算，公式见 docs/PRD.md 6.2。
 */
@Service
public class CreditService {

	private static final Logger log = LoggerFactory.getLogger(CreditService.class);

	private final ReputationRepository reputationRepository;
	private final ReviewRepository reviewRepository;
	private final OrderRepository orderRepository;
	private final ItemRepository itemRepository;
	private final UserRepository userRepository;
	private final CreditProperties properties;

	public CreditService(ReputationRepository reputationRepository,
			ReviewRepository reviewRepository,
			OrderRepository orderRepository,
			ItemRepository itemRepository,
			UserRepository userRepository,
			CreditProperties properties) {
		this.reputationRepository = reputationRepository;
		this.reviewRepository = reviewRepository;
		this.orderRepository = orderRepository;
		this.itemRepository = itemRepository;
		this.userRepository = userRepository;
		this.properties = properties;
	}

	public Reputation require(Long userId) {
		return reputationRepository.findByUserId(userId)
				.orElseGet(() -> reputationRepository.save(Reputation.forUser(userId)));
	}

	@Transactional
	public Reputation recompute(Long userId) {
		Reputation reputation = require(userId);
		int completed = (int) orderRepository.countAsParticipantByStatus(userId, OrderStatus.COMPLETED);
		int cancelled = (int) orderRepository.countAsParticipantByStatus(userId, OrderStatus.CANCELLED);
		int refunded = (int) orderRepository.countAsParticipantByStatus(userId, OrderStatus.REFUNDED);
		int totalDeals = completed + cancelled + refunded;

		List<Review> reviews = reviewRepository.findByRevieweeId(userId);
		int totalReviews = reviews.size();
		int good = (int) reviews.stream().filter(review -> review.getRating() >= 4).count();
		int bad = (int) reviews.stream().filter(review -> review.getRating() <= 2).count();
		double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0);

		User user = userRepository.findById(userId).orElse(null);
		int penalties = reputation.getDisputeCount() * 5 + reputation.getReportCount() * 3
				+ (user != null && user.getStatus() == UserStatus.BANNED ? 10 : 0);

		int score = computeScore(totalReviews, avg, completed, totalDeals, penalties);

		reputation.setTotalDeals(totalDeals);
		reputation.setCompletedDeals(completed);
		reputation.setCancelledDeals(cancelled);
		reputation.setRefundedDeals(refunded);
		reputation.setTotalReviews(totalReviews);
		reputation.setGoodReviews(good);
		reputation.setBadReviews(bad);
		reputation.setAvgRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
		reputation.setCreditScore(score);
		reputation.setBadge(CreditBadge.of(score));
		Reputation saved = reputationRepository.save(reputation);
		log.debug("信用分重算 userId={} score={} badge={}", userId, score, saved.getBadge());
		return saved;
	}

	int computeScore(int totalReviews, double avgRating, int completedDeals, int totalDeals, int penalties) {
		if (totalReviews == 0) {
			return clamp(properties.baseScore() - penalties);
		}
		double ratingPart = avgRating / 5.0 * 100;
		double dealPart = totalDeals == 0 ? 60.0 : completedDeals * 100.0 / totalDeals;
		double base = 0.6 * ratingPart + 0.4 * dealPart;
		return clamp((int) Math.round(base) - penalties);
	}

	/**
	 * 发布限制：低于阈值禁止发布，中等信用限制同时在售数量。
	 */
	public void assertCanPublish(Long userId) {
		Reputation reputation = require(userId);
		int score = reputation.getCreditScore();
		if (score < properties.minPublishThreshold()) {
			throw new BusinessException(ErrorCode.FORBIDDEN,
					"信用分过低（" + score + "），暂时无法发布商品");
		}
		if (score < properties.unlimitedPublishThreshold()) {
			long onSale = itemRepository.countBySeller_IdAndStatusIn(userId,
					List.of(ItemStatus.ON_SALE, ItemStatus.PENDING_REVIEW));
			if (onSale >= 3) {
				throw new BusinessException(ErrorCode.FORBIDDEN,
						"信用分 " + score + " 的用户同时最多发布 3 件商品，请先完成交易提升信用");
			}
		}
	}

	@Transactional
	public void addReportPenalty(Long userId) {
		Reputation reputation = require(userId);
		reputation.setReportCount(reputation.getReportCount() + 1);
		reputationRepository.save(reputation);
		recompute(userId);
	}

	@Transactional
	public void addDispute(Long userId) {
		Reputation reputation = require(userId);
		reputation.setDisputeCount(reputation.getDisputeCount() + 1);
		reputationRepository.save(reputation);
		recompute(userId);
	}

	private int clamp(int value) {
		return Math.max(0, Math.min(100, value));
	}
}
