package com.campusmarket.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
		@NotBlank(message = "原密码不能为空") String oldPassword,
		@NotBlank(message = "新密码不能为空")
		@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[\\x21-\\x7E]{8,32}$",
				message = "新密码需为 8-32 位，且同时包含字母与数字")
		String newPassword) {
}
