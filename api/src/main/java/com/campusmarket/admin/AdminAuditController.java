package com.campusmarket.admin;

import com.campusmarket.common.ApiResponse;
import com.campusmarket.common.PageResult;
import com.campusmarket.moderation.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-审计日志", description = "运营动作留痕")
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
public class AdminAuditController {

	private final AuditService auditService;

	public AdminAuditController(AuditService auditService) {
		this.auditService = auditService;
	}

	@Operation(summary = "审计日志分页")
	@GetMapping
	public ApiResponse<PageResult<AuditService.AuditView>> list(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.ok(PageResult.of(auditService.page(page, size), view -> view));
	}
}
