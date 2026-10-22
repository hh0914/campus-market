package com.campusmarket.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

	Optional<Favorite> findByUserIdAndItemId(Long userId, Long itemId);

	boolean existsByUserIdAndItemId(Long userId, Long itemId);

	long countByItemId(Long itemId);

	Page<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	List<Favorite> findByUserIdAndItemIdIn(Long userId, Collection<Long> itemIds);
}
