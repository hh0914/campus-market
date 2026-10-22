package com.campusmarket.catalog;

import com.campusmarket.auth.CurrentUser;
import com.campusmarket.catalog.dto.FavoriteToggleResponse;
import com.campusmarket.catalog.dto.ItemDetailResponse;
import com.campusmarket.catalog.dto.ItemRequest;
import com.campusmarket.catalog.dto.ItemSummaryResponse;
import com.campusmarket.common.BusinessException;
import com.campusmarket.common.ErrorCode;
import com.campusmarket.common.PageResult;
import com.campusmarket.credit.CreditService;
import com.campusmarket.credit.Reputation;
import com.campusmarket.moderation.AuditService;
import com.campusmarket.user.NotificationService;
import com.campusmarket.user.NotificationType;
import com.campusmarket.user.User;
import com.campusmarket.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ItemService {

	private static final Set<ItemStatus> EDITABLE = Set.of(ItemStatus.DRAFT, ItemStatus.REJECTED, ItemStatus.ON_SALE,
			ItemStatus.OFF_SHELF);

	private final ItemRepository itemRepository;
	private final CategoryRepository categoryRepository;
	private final FavoriteRepository favoriteRepository;
	private final UserRepository userRepository;
	private final CreditService creditService;
	private final NotificationService notificationService;
	private final CurrentUser currentUser;
	private final AuditService auditService;

	public ItemService(ItemRepository itemRepository,
			CategoryRepository categoryRepository,
			FavoriteRepository favoriteRepository,
			UserRepository userRepository,
			CreditService creditService,
			NotificationService notificationService,
			CurrentUser currentUser,
			AuditService auditService) {
		this.itemRepository = itemRepository;
		this.categoryRepository = categoryRepository;
		this.favoriteRepository = favoriteRepository;
		this.userRepository = userRepository;
		this.creditService = creditService;
		this.notificationService = notificationService;
		this.currentUser = currentUser;
		this.auditService = auditService;
	}

	@Transactional
	public ItemDetailResponse create(Long userId, ItemRequest request) {
		User seller = requireUser(userId);
		if (!seller.isVerified()) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "请先完成校园认证后再发布商品");
		}
		creditService.assertCanPublish(userId);

		Item item = new Item();
		item.setSeller(seller);
		applyRequest(item, request);
		if (request.submitNow()) {
			item.setStatus(ItemStatus.PENDING_REVIEW);
		}
		Item saved = itemRepository.save(item);
		return detail(saved.getId());
	}

	@Transactional
	public ItemDetailResponse update(Long userId, Long itemId, ItemRequest request) {
		Item item = requireItem(itemId);
		if (!item.getSellerId().equals(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "只能编辑自己发布的商品");
		}
		if (!EDITABLE.contains(item.getStatus())) {
			throw new BusinessException(ErrorCode.CONFLICT, "当前状态不允许编辑: " + item.getStatus());
		}
		applyRequest(item, request);
		if (request.submitNow() && item.getStatus() != ItemStatus.ON_SALE) {
			item.setStatus(ItemStatus.PENDING_REVIEW);
			item.setRejectReason(null);
		}
		itemRepository.save(item);
		return detail(itemId);
	}

	private void applyRequest(Item item, ItemRequest request) {
		Category category = categoryRepository.findById(request.categoryId())
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "分类不存在"));
		item.setCategory(category);
		item.setTitle(request.title().trim());
		item.setDescription(request.description().trim());
		item.setPrice(request.price());
		item.setOriginalPrice(request.originalPrice());
		item.setConditionLevel(request.conditionLevel());
		item.setTradeMethod(request.tradeMethod());
		item.setCampus(request.campus());
		item.setTags(request.tags());
		item.setRejectReason(null);

		item.clearImages();
		List<String> urls = request.imageUrls() == null ? List.of() : request.imageUrls();
		urls.stream().filter(url -> url != null && !url.isBlank()).forEach(item::addImage);
		item.setCoverUrl(item.getImages().isEmpty() ? null : item.getImages().get(0).getUrl());
	}

	@Transactional
	public ItemDetailResponse submit(Long userId, Long itemId) {
		Item item = requireItem(itemId);
		assertOwner(item, userId);
		if (item.getStatus() != ItemStatus.DRAFT && item.getStatus() != ItemStatus.REJECTED
				&& item.getStatus() != ItemStatus.OFF_SHELF) {
			throw new BusinessException(ErrorCode.CONFLICT, "当前状态不能提交审核");
		}
		creditService.assertCanPublish(userId);
		item.setStatus(ItemStatus.PENDING_REVIEW);
		item.setRejectReason(null);
		itemRepository.save(item);
		return detail(itemId);
	}

	@Transactional
	public void offShelf(Long userId, Long itemId) {
		Item item = requireItem(itemId);
		assertOwner(item, userId);
		if (item.getStatus() != ItemStatus.ON_SALE) {
			throw new BusinessException(ErrorCode.CONFLICT, "只有在售商品可以下架");
		}
		item.setStatus(ItemStatus.OFF_SHELF);
		itemRepository.save(item);
	}

	@Transactional
	public void approve(Long moderatorId, Long itemId) {
		Item item = requireItem(itemId);
		if (item.getStatus() != ItemStatus.PENDING_REVIEW) {
			throw new BusinessException(ErrorCode.CONFLICT, "商品不在待审核状态");
		}
		item.setStatus(ItemStatus.ON_SALE);
		item.setPublishedAt(java.time.LocalDateTime.now());
		item.setRejectReason(null);
		itemRepository.save(item);
		notificationService.push(item.getSellerId(), NotificationType.ITEM_AUDIT, "商品审核通过",
				"你的商品《" + item.getTitle() + "》已通过审核并上架", "/items/" + item.getId());
		auditService.log(moderatorId, "ITEM_APPROVE", "ITEM", itemId, "title=" + item.getTitle());
	}

	@Transactional
	public void reject(Long moderatorId, Long itemId, String reason) {
		if (reason == null || reason.trim().length() < 5) {
			throw new BusinessException(ErrorCode.INVALID_PARAM, "驳回理由至少 5 个字");
		}
		Item item = requireItem(itemId);
		if (item.getStatus() != ItemStatus.PENDING_REVIEW) {
			throw new BusinessException(ErrorCode.CONFLICT, "商品不在待审核状态");
		}
		item.setStatus(ItemStatus.REJECTED);
		item.setRejectReason(reason.trim());
		itemRepository.save(item);
		notificationService.push(item.getSellerId(), NotificationType.ITEM_AUDIT, "商品审核未通过",
				"《" + item.getTitle() + "》被驳回：" + reason.trim(), "/items/" + item.getId());
		auditService.log(moderatorId, "ITEM_REJECT", "ITEM", itemId, "reason=" + reason.trim());
	}

	@Transactional(readOnly = true)
	public PageResult<ItemSummaryResponse> pending(int page, int size) {
		Pageable pageable = PageRequest.of(Math.max(page - 1, 0), clampSize(size));
		Page<Item> result = itemRepository.findByStatusOrderByCreatedAtAsc(ItemStatus.PENDING_REVIEW, pageable);
		return PageResult.of(result, item -> ItemSummaryResponse.of(item, false));
	}

	@Transactional(readOnly = true)
	public PageResult<ItemSummaryResponse> search(String keyword, Long categoryId, ConditionLevel condition,
			BigDecimal minPrice, BigDecimal maxPrice, String sort, int page, int size, Long viewerId) {
		Specification<Item> specification = buildSpecification(keyword, categoryId, condition, minPrice, maxPrice,
				ItemStatus.ON_SALE);
		Pageable pageable = PageRequest.of(Math.max(page - 1, 0), clampSize(size), resolveSort(sort));
		Page<Item> result = itemRepository.findAll(specification, pageable);
		Set<Long> favoriteIds = favoritedItemIds(viewerId, result.getContent());
		return PageResult.of(result, item -> ItemSummaryResponse.of(item, favoriteIds.contains(item.getId())));
	}

	@Transactional
	public ItemDetailResponse detail(Long itemId) {
		Item item = requireItem(itemId);
		Long viewerId = currentUser.find().map(user -> user.id()).orElse(null);
		boolean favorited = viewerId != null && favoriteRepository.existsByUserIdAndItemId(viewerId, itemId);
		int bumped = item.getViewCount() + 1;
		itemRepository.incrementViewCount(itemId);
		item.setViewCount(bumped);

		Integer sellerCredit = item.getSeller() == null ? null
				: creditService.require(item.getSellerId()).getCreditScore();
		List<String> images = item.getImages().stream().map(ItemImage::getUrl).toList();
		return ItemDetailResponse.of(item, images, favorited, viewerId != null && viewerId.equals(item.getSellerId()),
				sellerCredit);
	}

	@Transactional(readOnly = true)
	public PageResult<ItemSummaryResponse> mine(Long userId, String statusFilter, int page, int size) {
		Pageable pageable = PageRequest.of(Math.max(page - 1, 0), clampSize(size));
		Page<Item> result;
		if (statusFilter == null || statusFilter.isBlank()) {
			result = itemRepository.findBySeller_IdOrderByCreatedAtDesc(userId, pageable);
		}
		else {
			ItemStatus status = ItemStatus.valueOf(statusFilter.toUpperCase());
			result = itemRepository.findBySeller_IdAndStatusInOrderByCreatedAtDesc(userId, List.of(status), pageable);
		}
		return PageResult.of(result, item -> ItemSummaryResponse.of(item, false));
	}

	@Transactional(readOnly = true)
	public PageResult<ItemSummaryResponse> bySeller(Long sellerId, int page, int size) {
		Pageable pageable = PageRequest.of(Math.max(page - 1, 0), clampSize(size));
		Page<Item> result = itemRepository.findBySeller_IdAndStatusInOrderByCreatedAtDesc(sellerId,
				List.of(ItemStatus.ON_SALE), pageable);
		return PageResult.of(result, item -> ItemSummaryResponse.of(item, false));
	}

	@Transactional(readOnly = true)
	public List<ItemSummaryResponse> related(Long itemId) {
		Item item = requireItem(itemId);
		return itemRepository
				.findTop6ByCategoryIdAndStatusAndIdNotOrderByCreatedAtDesc(item.getCategory().getId(),
						ItemStatus.ON_SALE, itemId)
				.stream()
				.map(related -> ItemSummaryResponse.of(related, false))
				.toList();
	}

	@Transactional
	public FavoriteToggleResponse toggleFavorite(Long userId, Long itemId) {
		Item item = requireItem(itemId);
		var existing = favoriteRepository.findByUserIdAndItemId(userId, itemId);
		boolean favorited;
		if (existing.isPresent()) {
			favoriteRepository.delete(existing.get());
			itemRepository.updateFavoriteCount(itemId, -1);
			item.setFavoriteCount(Math.max(0, item.getFavoriteCount() - 1));
			favorited = false;
		}
		else {
			Favorite favorite = new Favorite();
			favorite.setUserId(userId);
			favorite.setItemId(itemId);
			favoriteRepository.save(favorite);
			itemRepository.updateFavoriteCount(itemId, 1);
			item.setFavoriteCount(item.getFavoriteCount() + 1);
			favorited = true;
		}
		return new FavoriteToggleResponse(favorited, item.getFavoriteCount());
	}

	@Transactional(readOnly = true)
	public PageResult<ItemSummaryResponse> myFavorites(Long userId, int page, int size) {
		Pageable pageable = PageRequest.of(Math.max(page - 1, 0), clampSize(size));
		Page<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
		List<ItemSummaryResponse> list = favorites.getContent().stream()
				.map(favorite -> itemRepository.findById(favorite.getItemId()))
				.flatMap(java.util.Optional::stream)
				.map(item -> ItemSummaryResponse.of(item, true))
				.toList();
		return new PageResult<>(list, favorites.getTotalElements(), page, clampSize(size),
				favorites.getTotalPages());
	}

	private Set<Long> favoritedItemIds(Long viewerId, List<Item> items) {
		if (viewerId == null || items.isEmpty()) {
			return Set.of();
		}
		List<Long> ids = items.stream().map(Item::getId).toList();
		return favoriteRepository.findByUserIdAndItemIdIn(viewerId, ids).stream()
				.map(Favorite::getItemId)
				.collect(java.util.stream.Collectors.toSet());
	}

	private Specification<Item> buildSpecification(String keyword, Long categoryId, ConditionLevel condition,
			BigDecimal minPrice, BigDecimal maxPrice, ItemStatus status) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.equal(root.get("status"), status));
			if (keyword != null && !keyword.isBlank()) {
				String like = "%" + keyword.trim().toLowerCase() + "%";
				predicates.add(cb.or(
						cb.like(cb.lower(root.get("title")), like),
						cb.like(cb.lower(root.get("description")), like)));
			}
			if (categoryId != null) {
				predicates.add(cb.equal(root.get("category").get("id"), categoryId));
			}
			if (condition != null) {
				predicates.add(cb.equal(root.get("conditionLevel"), condition));
			}
			if (minPrice != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
			}
			if (maxPrice != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
			}
			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	private Sort resolveSort(String sort) {
		if (sort == null) {
			return Sort.by(Sort.Direction.DESC, "createdAt");
		}
		return switch (sort) {
			case "priceAsc" -> Sort.by(Sort.Direction.ASC, "price");
			case "priceDesc" -> Sort.by(Sort.Direction.DESC, "price");
			case "hot" -> Sort.by(Sort.Direction.DESC, "viewCount");
			default -> Sort.by(Sort.Direction.DESC, "createdAt");
		};
	}

	private int clampSize(int size) {
		if (size <= 0) {
			return 12;
		}
		return Math.min(size, 50);
	}

	private void assertOwner(Item item, Long userId) {
		if (!item.getSellerId().equals(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "只能操作自己发布的商品");
		}
	}

	public Item requireItem(Long itemId) {
		return itemRepository.findById(itemId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "商品不存在"));
	}

	private User requireUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
	}

	public Reputation reputationOf(Long userId) {
		return creditService.require(userId);
	}
}
