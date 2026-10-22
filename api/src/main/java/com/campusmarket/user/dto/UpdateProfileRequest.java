package com.campusmarket.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
		@Size(min = 2, max = 32, message = "昵称需为 2-32 个字符") String nickname,
		@Size(max = 255, message = "头像地址过长") String avatarUrl,
		@Size(max = 200, message = "简介最多 200 个字符") String bio,
		@Size(max = 50, message = "校区名称过长") String campus) {
}
