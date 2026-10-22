package com.campusmarket.common;

import java.util.List;
import java.util.function.Function;

/**
 * 统一分页返回结构，前端表格组件直接消费。
 */
public record PageResult<T>(List<T> list, long total, int page, int size, int pages) {

	public static <E, T> PageResult<T> of(org.springframework.data.domain.Page<E> page, Function<E, T> mapper) {
		return new PageResult<>(
				page.getContent().stream().map(mapper).toList(),
				page.getTotalElements(),
				page.getNumber() + 1,
				page.getSize(),
				page.getTotalPages());
	}

	public static <T> PageResult<T> empty(int page, int size) {
		return new PageResult<>(List.of(), 0, page, size, 0);
	}
}
