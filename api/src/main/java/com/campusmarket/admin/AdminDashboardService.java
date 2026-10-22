package com.campusmarket.admin;

import com.campusmarket.admin.dto.CategoryStatResponse;
import com.campusmarket.admin.dto.DashboardSummaryResponse;
import com.campusmarket.admin.dto.TrendPointResponse;
import com.campusmarket.catalog.Category;
import com.campusmarket.catalog.CategoryRepository;
import com.campusmarket.catalog.Item;
import com.campusmarket.catalog.ItemRepository;
import com.campusmarket.catalog.ItemStatus;
import com.campusmarket.moderation.ReportRepository;
import com.campusmarket.moderation.ReportStatus;
import com.campusmarket.trade.Order;
import com.campusmarket.trade.OrderRepository;
import com.campusmarket.trade.OrderStatus;
import com.campusmarket.user.User;
import com.campusmarket.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据看板：实时聚合查询，数据量为教学演示规模。
 */
@Service
public class AdminDashboardService {

	private final UserRepository userRepository;
	private final ItemRepository itemRepository;
	private final OrderRepository orderRepository;
	private final ReportRepository reportRepository;
	private final CategoryRepository categoryRepository;

	public AdminDashboardService(UserRepository userRepository,
			ItemRepository itemRepository,
			OrderRepository orderRepository,
			ReportRepository reportRepository,
			CategoryRepository categoryRepository) {
		this.userRepository = userRepository;
		this.itemRepository = itemRepository;
		this.orderRepository = orderRepository;
		this.reportRepository = reportRepository;
		this.categoryRepository = categoryRepository;
	}

	@Transactional(readOnly = true)
	public DashboardSummaryResponse summary() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime weekAgo = now.minusDays(7);
		LocalDateTime todayStart = LocalDate.now().atStartOfDay();
		return new DashboardSummaryResponse(
				userRepository.count(),
				userRepository.countCreatedAfter(weekAgo),
				itemRepository.countByStatus(ItemStatus.ON_SALE),
				itemRepository.countByStatus(ItemStatus.PENDING_REVIEW),
				reportRepository.countByStatus(ReportStatus.PENDING),
				orderRepository.count(),
				orderRepository.countByStatus(OrderStatus.COMPLETED),
				orderRepository.sumCompletedGmv(),
				orderRepository.sumGmvAfter(weekAgo),
				orderRepository.countByCreatedAtAfter(todayStart),
				userRepository.countCreatedAfter(todayStart));
	}

	@Transactional(readOnly = true)
	public List<TrendPointResponse> trend(int days) {
		int span = Math.min(30, Math.max(7, days));
		LocalDate startDate = LocalDate.now().minusDays(span - 1);
		LocalDateTime from = startDate.atStartOfDay();

		Map<LocalDate, Long> orders = new LinkedHashMap<>();
		Map<LocalDate, BigDecimal> gmv = new LinkedHashMap<>();
		Map<LocalDate, Long> users = new LinkedHashMap<>();
		Map<LocalDate, Long> items = new LinkedHashMap<>();

		for (Order order : orderRepository.findByCreatedAtAfter(from)) {
			LocalDate day = order.getCreatedAt().toLocalDate();
			orders.merge(day, 1L, Long::sum);
			if (order.getStatus() == OrderStatus.COMPLETED && order.getCompletedAt() != null) {
				gmv.merge(order.getCompletedAt().toLocalDate(), order.getAmount(), BigDecimal::add);
			}
		}
		for (User user : userRepository.findByCreatedAtAfter(from)) {
			users.merge(user.getCreatedAt().toLocalDate(), 1L, Long::sum);
		}
		for (Item item : itemRepository.findByCreatedAtAfter(from)) {
			items.merge(item.getCreatedAt().toLocalDate(), 1L, Long::sum);
		}

		List<TrendPointResponse> points = new ArrayList<>(span);
		for (int i = 0; i < span; i++) {
			LocalDate day = startDate.plusDays(i);
			points.add(new TrendPointResponse(
					day.toString(),
					orders.getOrDefault(day, 0L),
					gmv.getOrDefault(day, BigDecimal.ZERO),
					users.getOrDefault(day, 0L),
					items.getOrDefault(day, 0L)));
		}
		return points;
	}

	@Transactional(readOnly = true)
	public List<CategoryStatResponse> categoryStats() {
		return itemRepository.countSoldGroupByCategory().stream()
				.map(row -> {
					Long categoryId = ((Number) row[0]).longValue();
					long count = ((Number) row[1]).longValue();
					BigDecimal amount = row[2] == null ? BigDecimal.ZERO : new BigDecimal(row[2].toString());
					String name = categoryRepository.findById(categoryId).map(Category::getName).orElse("未分类");
					return new CategoryStatResponse(categoryId, name, count, amount);
				})
				.toList();
	}
}
