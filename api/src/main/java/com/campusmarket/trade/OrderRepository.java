package com.campusmarket.trade;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

	Page<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);

	Page<Order> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);

	Page<Order> findByBuyerIdAndStatusOrderByCreatedAtDesc(Long buyerId, OrderStatus status, Pageable pageable);

	Page<Order> findBySellerIdAndStatusOrderByCreatedAtDesc(Long sellerId, OrderStatus status, Pageable pageable);

	List<Order> findTop50ByStatusAndPayDeadlineBefore(OrderStatus status, LocalDateTime deadline);

	List<Order> findByItemIdAndStatusIn(Long itemId, List<OrderStatus> statuses);

	long countByStatus(OrderStatus status);

	long countByCreatedAtAfter(LocalDateTime from);

	java.util.List<Order> findByCreatedAtAfter(LocalDateTime from);

	@Query("select coalesce(sum(o.amount), 0) from Order o where o.status = 'COMPLETED'")
	BigDecimal sumCompletedGmv();

	@Query("select coalesce(sum(o.amount), 0) from Order o where o.status = 'COMPLETED' and o.completedAt >= :from")
	BigDecimal sumGmvAfter(@Param("from") LocalDateTime from);

	long countByStatusAndCompletedAtAfter(OrderStatus status, LocalDateTime from);

	long countByStatusAndCreatedAtAfter(OrderStatus status, LocalDateTime from);

	@Query("select count(o) from Order o where (o.buyerId = :userId or o.sellerId = :userId) and o.status = :status")
	long countAsParticipantByStatus(@Param("userId") Long userId, @Param("status") OrderStatus status);
}
