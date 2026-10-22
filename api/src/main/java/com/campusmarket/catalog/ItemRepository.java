package com.campusmarket.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

	Page<Item> findBySeller_IdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);

	Page<Item> findBySeller_IdAndStatusInOrderByCreatedAtDesc(Long sellerId, Collection<ItemStatus> statuses, Pageable pageable);

	long countBySeller_IdAndStatusIn(Long sellerId, Collection<ItemStatus> statuses);

	long countByStatus(ItemStatus status);

	long countByCategoryId(Long categoryId);

	java.util.List<Item> findByCreatedAtAfter(java.time.LocalDateTime from);

	Page<Item> findByStatusOrderByCreatedAtAsc(ItemStatus status, Pageable pageable);

	List<Item> findTop6ByCategoryIdAndStatusAndIdNotOrderByCreatedAtDesc(Long categoryId, ItemStatus status, Long excludedId);

	@Modifying
	@Query("update Item i set i.viewCount = i.viewCount + 1 where i.id = :id")
	int incrementViewCount(@Param("id") Long id);

	@Modifying
	@Query("update Item i set i.favoriteCount = i.favoriteCount + :delta where i.id = :id")
	int updateFavoriteCount(@Param("id") Long id, @Param("delta") int delta);

	@Query("select count(i) from Item i where i.status = :status and i.publishedAt >= :from")
	long countPublishedAfter(@Param("status") ItemStatus status, @Param("from") java.time.LocalDateTime from);

	@Query("select i.category.id, count(i), coalesce(sum(i.price), 0) from Item i where i.status = 'SOLD' group by i.category.id")
	List<Object[]> countSoldGroupByCategory();
}
