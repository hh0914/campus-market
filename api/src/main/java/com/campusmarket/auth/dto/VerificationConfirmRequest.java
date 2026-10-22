package com.campusmarket.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificationConfirmRequest(
		@NotBlank(message = "验证码不能为空")
		@Pattern(regexp = "^\\d{6}$", message = "验证码为 6 位数字")
		String code) {
}
