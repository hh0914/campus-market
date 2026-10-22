package com.campusmarket.chat;

import com.campusmarket.auth.CurrentUser;
import com.campusmarket.chat.dto.ConversationResponse;
import com.campusmarket.chat.dto.MessageResponse;
import com.campusmarket.chat.dto.SendMessageRequest;
import com.campusmarket.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "聊天", description = "议价会话与消息")
@RestController
@RequestMapping("/api/v1/conversations")
public class ChatController {

	private final ChatService chatService;
	private final CurrentUser currentUser;

	public ChatController(ChatService chatService, CurrentUser currentUser) {
		this.chatService = chatService;
		this.currentUser = currentUser;
	}

	@Operation(summary = "按商品打开会话")
	@PostMapping
	public ApiResponse<ConversationResponse> open(@RequestBody Map<String, Long> body) {
		Long itemId = body.get("itemId");
		return ApiResponse.ok(chatService.open(currentUser.requireId(), itemId));
	}

	@Operation(summary = "我的会话列表")
	@GetMapping
	public ApiResponse<List<ConversationResponse>> list() {
		return ApiResponse.ok(chatService.list(currentUser.requireId()));
	}

	@Operation(summary = "拉取消息")
	@GetMapping("/{id}/messages")
	public ApiResponse<List<MessageResponse>> messages(@PathVariable Long id,
			@RequestParam(required = false) Long afterId) {
		return ApiResponse.ok(chatService.messages(currentUser.requireId(), id, afterId));
	}

	@Operation(summary = "发送消息")
	@PostMapping("/{id}/messages")
	public ApiResponse<MessageResponse> send(@PathVariable Long id, @Valid @RequestBody SendMessageRequest request) {
		return ApiResponse.ok(chatService.send(currentUser.requireId(), id, request));
	}

	@Operation(summary = "标记已读")
	@PostMapping("/{id}/read")
	public ApiResponse<Void> markRead(@PathVariable Long id) {
		chatService.markRead(currentUser.requireId(), id);
		return ApiResponse.ok();
	}
}
