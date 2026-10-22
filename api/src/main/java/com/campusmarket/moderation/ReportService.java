package com.campusmarket.moderation;

import com.campusmarket.catalog.Item;
import com.campusmarket.catalog.ItemRepository;
import com.campusmarket.catalog.ItemStatus;
import com.campusmarket.common.BusinessException;
import com.campusmarket.common.ErrorCode;
import com.campusmarket.common.PageResult;
import com.campusmarket.credit.CreditService;
import com.campusmarket.moderation.dto.CreateReportRequest;
import com.campusmarket.moderation.dto.HandleReportRequest;
import com.campusmarket.moderation.dto.ReportResponse;
import com.campusmarket.user.NotificationService;
import com.campusmarket.user.NotificationType;
import com.campusmarket.user.User;
import com.campusmarket.user.UserRepository;
import com.campusmarket.user.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ReportService {

	private final ReportRepository reportRepository;
	private final ItemRepository itemRepository;
	private final UserRepository userRepository;
	private final CreditService creditService;
	private final NotificationService notificationService;
	private final AuditService auditService;

	public ReportService(ReportRepository reportRepository,
			ItemRepository itemRepository,
			UserRepository userRepository,
			CreditService creditService,
			NotificationService notificationService,
			AuditService auditService) {
		this.reportRepository = reportRepository;
		this.itemRepository = itemRepository;
		this.userRepository = userRepository;
		this.creditService = creditService;
		this.notificationService = notificationService;
		this.auditService = auditService;
	}

	@Transactional
	public ReportResponse create(Long reporterId, CreateReportRequest request) {
		if (request.targetType() == ReportTargetType.ITEM) {
			Item item = itemRepository.findById(request.targetId())
					.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "被举报的商品不存在"));
			if (item.getSellerId().equals(reporterId)) {
				throw new BusinessException(ErrorCode.BUSINESS_ERROR, "不能举报自己的商品");
			}
		}
		else {
			if (request.targetId().equals(reporterId)) {
				throw new BusinessException(ErrorCode.BUSINESS_ERROR, "不能举报自己");
			}
			userRepository.findById(request.targetId())
					.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "被举报的用户不存在"));
		}
		long pending = reportRepository.countByTargetTypeAndTargetIdAndStatus(request.targetType(),
				request.targetId(), ReportStatus.PENDING);
		if (pending > 0) {
			throw new BusinessException(ErrorCode.CONFLICT, "该对象已有待处理举报，请等待处理结果");
		}

		Report report = new Report();
		report.setReporterId(reporterId);
		report.setTargetType(request.targetType());
		report.setTargetId(request.targetId());
		report.setReasonType(request.reasonType());
		report.setDescription(request.description().trim());
		report.setEvidenceUrl(request.evidenceUrl());
		report.setStatus(ReportStatus.PENDING);
		reportRepository.save(report);
		return toResponse(report);
	}

	@Transactional(readOnly = true)
	public PageResult<ReportResponse> page(String statusFilter, int page, int size) {
		var pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 50));
		Page<Report> result;
		if (statusFilter == null || statusFilter.isBlank()) {
			result = reportRepository.findAllByOrderByCreatedAtDesc(pageable);
		}
		else {
			result = reportRepository.findByStatusOrderByCreatedAtAsc(ReportStatus.valueOf(statusFilter.toUpperCase()),
					pageable);
		}
		return PageResult.of(result, this::toResponse);
	}

	@Transactional(readOnly = true)
	public PageResult<ReportResponse> mine(Long reporterId, int page, int size) {
		var pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 50));
		return PageResult.of(reportRepository.findByReporterIdOrderByCreatedAtDesc(reporterId, pageable),
				this::toResponse);
	}

	@Transactional
	public ReportResponse claim(Long handlerId, Long reportId) {
		Report report = require(reportId);
		if (report.getStatus() != ReportStatus.PENDING) {
			throw new BusinessException(ErrorCode.CONFLICT, "只有待处理工单可以领取");
		}
		report.setStatus(ReportStatus.PROCESSING);
		report.setHandlerId(handlerId);
		reportRepository.save(report);
		return toResponse(report);
	}

	@Transactional
	public ReportResponse resolve(Long handlerId, Long reportId, HandleReportRequest request) {
		Report report = require(reportId);
		if (report.getStatus() != ReportStatus.PROCESSING && report.getStatus() != ReportStatus.PENDING) {
			throw new BusinessException(ErrorCode.CONFLICT, "该工单已处理");
		}
		report.setStatus(ReportStatus.RESOLVED);
		report.setHandlerId(handlerId);
		report.setHandleRemark(request.remark().trim());
		report.setHandledAt(LocalDateTime.now());

		switch (request.action()) {
			case DOWN_ITEM -> {
				if (report.getTargetType() != ReportTargetType.ITEM) {
					throw new BusinessException(ErrorCode.INVALID_PARAM, "只有商品举报可以下架商品");
				}
				Item item = itemRepository.findById(report.getTargetId())
						.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "商品不存在"));
				item.setStatus(ItemStatus.OFF_SHELF);
				itemRepository.save(item);
				creditService.addReportPenalty(item.getSellerId());
				notificationService.push(item.getSellerId(), NotificationType.REPORT, "商品被下架",
						"你的商品《" + item.getTitle() + "》因举报成立被下架：" + request.remark(), "/items/" + item.getId());
			}
			case WARN -> {
				Long targetUserId = resolveUserId(report);
				User user = requireUser(targetUserId);
				user.setStatus(UserStatus.WARNED);
				userRepository.save(user);
				notificationService.push(targetUserId, NotificationType.REPORT, "收到平台警告",
						"你的账号因违规收到警告：" + request.remark(), null);
			}
			case BAN -> {
				Long targetUserId = resolveUserId(report);
				User user = requireUser(targetUserId);
				user.setStatus(UserStatus.BANNED);
				user.setBannedUntil(LocalDateTime.now().plusDays(7));
				userRepository.save(user);
				creditService.addReportPenalty(targetUserId);
				notificationService.push(targetUserId, NotificationType.REPORT, "账号被封禁",
						"你的账号因违规被封禁 7 天：" + request.remark(), null);
			}
			case NONE -> {
				// 仅记录处理意见，不做额外处置
			}
		}

		reportRepository.save(report);
		auditService.log(handlerId, "REPORT_RESOLVE", "REPORT", reportId,
				"action=" + request.action() + ", remark=" + request.remark());
		notificationService.push(report.getReporterId(), NotificationType.REPORT, "举报已处理",
				"你提交的举报已处理：" + request.remark(), null);
		return toResponse(report);
	}

	@Transactional
	public ReportResponse reject(Long handlerId, Long reportId, String remark) {
		if (remark == null || remark.trim().length() < 3) {
			throw new BusinessException(ErrorCode.INVALID_PARAM, "驳回意见至少 3 个字");
		}
		Report report = require(reportId);
		if (report.getStatus() == ReportStatus.REJECTED || report.getStatus() == ReportStatus.RESOLVED) {
			throw new BusinessException(ErrorCode.CONFLICT, "该工单已处理");
		}
		report.setStatus(ReportStatus.REJECTED);
		report.setHandlerId(handlerId);
		report.setHandleRemark(remark.trim());
		report.setHandledAt(LocalDateTime.now());
		reportRepository.save(report);
		auditService.log(handlerId, "REPORT_REJECT", "REPORT", reportId, "remark=" + remark);
		notificationService.push(report.getReporterId(), NotificationType.REPORT, "举报未通过",
				"你提交的举报经核实未成立：" + remark, null);
		return toResponse(report);
	}

	private Long resolveUserId(Report report) {
		if (report.getTargetType() == ReportTargetType.USER) {
			return report.getTargetId();
		}
		Item item = itemRepository.findById(report.getTargetId())
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "商品不存在"));
		return item.getSellerId();
	}

	private ReportResponse toResponse(Report report) {
		String targetTitle;
		if (report.getTargetType() == ReportTargetType.ITEM) {
			targetTitle = itemRepository.findById(report.getTargetId()).map(Item::getTitle).orElse("商品已删除");
		}
		else {
			targetTitle = userRepository.findById(report.getTargetId()).map(User::getNickname).orElse("用户已注销");
		}
		String reporter = userRepository.findById(report.getReporterId()).map(User::getNickname).orElse("未知");
		String handler = report.getHandlerId() == null ? null
				: userRepository.findById(report.getHandlerId()).map(User::getNickname).orElse("未知");
		return ReportResponse.of(report, reporter, targetTitle, handler);
	}

	private Report require(Long reportId) {
		return reportRepository.findById(reportId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "工单不存在"));
	}

	private User requireUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
	}
}
