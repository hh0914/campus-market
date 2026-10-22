package com.campusmarket.moderation.dto;

import com.campusmarket.moderation.Report;
import com.campusmarket.moderation.ReportReason;
import com.campusmarket.moderation.ReportStatus;
import com.campusmarket.moderation.ReportTargetType;

import java.time.LocalDateTime;

public record ReportResponse(Long id, Long reporterId, String reporterNickname, ReportTargetType targetType,
		Long targetId, String targetTitle, ReportReason reasonType, String reasonLabel, String description,
		String evidenceUrl, ReportStatus status, Long handlerId, String handlerNickname, String handleRemark,
		LocalDateTime handledAt, LocalDateTime createdAt) {

	public static ReportResponse of(Report report, String reporterNickname, String targetTitle,
			String handlerNickname) {
		return new ReportResponse(report.getId(), report.getReporterId(), reporterNickname, report.getTargetType(),
				report.getTargetId(), targetTitle, report.getReasonType(), report.getReasonType().getLabel(),
				report.getDescription(), report.getEvidenceUrl(), report.getStatus(), report.getHandlerId(),
				handlerNickname, report.getHandleRemark(), report.getHandledAt(), report.getCreatedAt());
	}
}
