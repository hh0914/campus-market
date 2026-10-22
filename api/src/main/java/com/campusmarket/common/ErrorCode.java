package com.campusmarket.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

	SUCCESS(0, HttpStatus.OK),
	INVALID_PARAM(40000, HttpStatus.BAD_REQUEST),
	BUSINESS_ERROR(40001, HttpStatus.BAD_REQUEST),
	UNAUTHORIZED(40100, HttpStatus.UNAUTHORIZED),
	TOKEN_EXPIRED(40101, HttpStatus.UNAUTHORIZED),
	TOKEN_INVALID(40102, HttpStatus.UNAUTHORIZED),
	FORBIDDEN(40300, HttpStatus.FORBIDDEN),
	NOT_FOUND(40400, HttpStatus.NOT_FOUND),
	CONFLICT(40900, HttpStatus.CONFLICT),
	PAYLOAD_TOO_LARGE(41300, HttpStatus.PAYLOAD_TOO_LARGE),
	TOO_MANY_REQUESTS(42900, HttpStatus.TOO_MANY_REQUESTS),
	INTERNAL_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR);

	private final int code;
	private final HttpStatus httpStatus;

	ErrorCode(int code, HttpStatus httpStatus) {
		this.code = code;
		this.httpStatus = httpStatus;
	}

	public int getCode() {
		return code;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}
}
