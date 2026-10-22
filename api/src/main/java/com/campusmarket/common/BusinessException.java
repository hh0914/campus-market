package com.campusmarket.common;

public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public BusinessException(String message) {
		this(ErrorCode.BUSINESS_ERROR, message);
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}
}
