package com.campusmarket.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	List<Category> findByEnabledTrueOrderBySortOrderAsc();

	List<Category> findAllByOrderBySortOrderAsc();

	boolean existsBySlug(String slug);

	Optional<Category> findBySlug(String slug);
}
