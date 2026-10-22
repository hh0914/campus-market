package com.campusmarket.common;

import org.slf4j.MDC;

/**
 * 统一响应体。code 为 0 表示成功，其余为业务错误码。
 */
public record ApiResponse<T>(int code, String message, T data, String traceId) {

	public static final int SUCCESS_CODE = 0;

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(SUCCESS_CODE, "ok", data, currentTraceId());
	}

	public static ApiResponse<Void> ok() {
		return new ApiResponse<>(SUCCESS_CODE, "ok", null, currentTraceId());
	}

	public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
		return new ApiResponse<>(errorCode.getCode(), message, null, currentTraceId());
	}

	private static String currentTraceId() {
		return MDC.get(TraceIdFilter.TRACE_ID);
	}
}
