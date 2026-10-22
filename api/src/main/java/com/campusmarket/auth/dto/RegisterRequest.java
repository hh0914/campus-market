package com.campusmarket.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank(message = "学号不能为空")
		@Pattern(regexp = "^[0-9A-Za-z]{6,20}$", message = "学号需为 6-20 位字母或数字")
		String studentNo,

		@NotBlank(message = "昵称不能为空")
		@Size(max = 32, message = "昵称最多 32 个字符")
		String nickname,

		@NotBlank(message = "邮箱不能为空")
		@Email(message = "邮箱格式不正确")
		String email,

		@NotBlank(message = "密码不能为空")
		@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[\\x21-\\x7E]{8,32}$",
				message = "密码需为 8-32 位，且同时包含字母与数字")
		String password) {
}
