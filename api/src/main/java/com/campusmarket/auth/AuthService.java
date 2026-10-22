package com.campusmarket.auth;

import com.campusmarket.auth.dto.LoginRequest;
import com.campusmarket.auth.dto.RegisterRequest;
import com.campusmarket.auth.dto.TokenResponse;
import com.campusmarket.auth.dto.UserSummary;
import com.campusmarket.auth.dto.VerificationSendResponse;
import com.campusmarket.common.BusinessException;
import com.campusmarket.common.ErrorCode;
import com.campusmarket.config.DemoProperties;
import com.campusmarket.credit.Reputation;
import com.campusmarket.credit.ReputationRepository;
import com.campusmarket.trade.Wallet;
import com.campusmarket.trade.WalletRepository;
import com.campusmarket.user.Role;
import com.campusmarket.user.User;
import com.campusmarket.user.UserRepository;
import com.campusmarket.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);
	private static final int MAX_ATTEMPTS = 5;
	private static final int CODE_TTL_MINUTES = 10;

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final StudentVerificationRepository verificationRepository;
	private final ReputationRepository reputationRepository;
	private final WalletRepository walletRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final DemoProperties demoProperties;
	private final SecureRandom random = new SecureRandom();

	public AuthService(UserRepository userRepository,
			RefreshTokenRepository refreshTokenRepository,
			StudentVerificationRepository verificationRepository,
			ReputationRepository reputationRepository,
			WalletRepository walletRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			DemoProperties demoProperties) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.verificationRepository = verificationRepository;
		this.reputationRepository = reputationRepository;
		this.walletRepository = walletRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.demoProperties = demoProperties;
	}

	@Transactional
	public TokenResponse register(RegisterRequest request, String userAgent) {
		if (userRepository.existsByStudentNo(request.studentNo())) {
			throw new BusinessException(ErrorCode.CONFLICT, "该学号已注册");
		}
		if (userRepository.existsByEmail(request.email())) {
			throw new BusinessException(ErrorCode.CONFLICT, "该邮箱已注册");
		}
		User user = new User();
		user.setStudentNo(request.studentNo());
		user.setNickname(request.nickname());
		user.setEmail(request.email());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setRole(Role.STUDENT);
		user.setStatus(UserStatus.ACTIVE);
		user.setVerified(false);
		userRepository.save(user);

		walletRepository.save(Wallet.forUser(user.getId()));
		reputationRepository.save(Reputation.forUser(user.getId()));

		log.info("新用户注册 id={} studentNo={}", user.getId(), user.getStudentNo());
		return issueTokens(user, userAgent);
	}

	@Transactional
	public TokenResponse login(LoginRequest request, String userAgent) {
		User user = userRepository.findByStudentNo(request.account())
				.or(() -> userRepository.findByEmail(request.account()))
				.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误"));
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
		}
		if (!user.isActive()) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被封禁");
		}
		user.setLastLoginAt(LocalDateTime.now());
		userRepository.save(user);
		return issueTokens(user, userAgent);
	}

	@Transactional
	public TokenResponse refresh(String rawRefreshToken, String userAgent) {
		RefreshToken stored = refreshTokenRepository.findByTokenHash(sha256(rawRefreshToken))
				.orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID, "刷新令牌无效"));
		if (stored.isRevoked() || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "刷新令牌已失效，请重新登录");
		}
		stored.setRevoked(true);
		refreshTokenRepository.save(stored);

		User user = userRepository.findById(stored.getUserId())
				.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在"));
		if (!user.isActive()) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被封禁");
		}
		return issueTokens(user, userAgent);
	}

	@Transactional
	public void logout(Long userId) {
		refreshTokenRepository.revokeAllByUserId(userId);
	}

	@Transactional
	public VerificationSendResponse sendVerification(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
		if (user.isVerified()) {
			throw new BusinessException(ErrorCode.CONFLICT, "该账号已完成校园认证");
		}
		String code = String.format("%06d", random.nextInt(1_000_000));
		StudentVerification verification = new StudentVerification();
		verification.setUserId(user.getId());
		verification.setStudentNo(user.getStudentNo());
		verification.setSchoolEmail(user.getEmail());
		verification.setCode(code);
		verification.setExpiresAt(LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES));
		verificationRepository.save(verification);

		log.warn("校园认证验证码 userId={} email={} code={} 有效期 {} 分钟（演示环境仅打印日志）",
				user.getId(), user.getEmail(), code, CODE_TTL_MINUTES);
		return new VerificationSendResponse(user.getEmail(), CODE_TTL_MINUTES * 60L,
				"验证码已发送至校内邮箱，演示环境同时显示在下方",
				demoProperties.exposeVerificationCode() ? code : null);
	}

	@Transactional
	public UserSummary confirmVerification(Long userId, String code) {
		StudentVerification verification = verificationRepository
				.findFirstByUserIdAndStatusOrderByIdDesc(userId, VerificationStatus.PENDING)
				.orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_ERROR, "请先获取验证码"));
		if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
			verification.setStatus(VerificationStatus.FAILED);
			verificationRepository.save(verification);
			throw new BusinessException(ErrorCode.BUSINESS_ERROR, "验证码已过期，请重新获取");
		}
		if (verification.getAttemptCount() >= MAX_ATTEMPTS) {
			verification.setStatus(VerificationStatus.FAILED);
			verificationRepository.save(verification);
			throw new BusinessException(ErrorCode.BUSINESS_ERROR, "错误次数过多，请重新获取验证码");
		}
		if (!verification.getCode().equals(code)) {
			verification.setAttemptCount(verification.getAttemptCount() + 1);
			verificationRepository.save(verification);
			throw new BusinessException(ErrorCode.BUSINESS_ERROR, "验证码不正确");
		}
		verification.setStatus(VerificationStatus.VERIFIED);
		verification.setVerifiedAt(LocalDateTime.now());
		verificationRepository.save(verification);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
		user.setVerified(true);
		userRepository.save(user);
		log.info("校园认证通过 userId={} studentNo={}", userId, user.getStudentNo());
		return UserSummary.of(user, 80, java.math.BigDecimal.ZERO);
	}

	public UserSummary summary(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
		Reputation reputation = reputationRepository.findByUserId(userId).orElse(null);
		return UserSummary.of(user,
				reputation == null ? null : reputation.getCreditScore(),
				reputation == null ? null : reputation.getAvgRating());
	}

	private TokenResponse issueTokens(User user, String userAgent) {
		String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name(), user.isVerified());
		String rawRefresh = randomToken();
		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setUserId(user.getId());
		refreshToken.setTokenHash(sha256(rawRefresh));
		refreshToken.setExpiresAt(LocalDateTime.now().plusDays(jwtService.refreshTokenDays()));
		refreshToken.setUserAgent(userAgent == null ? null : userAgent.substring(0, Math.min(userAgent.length(), 200)));
		refreshTokenRepository.save(refreshToken);

		Reputation reputation = reputationRepository.findByUserId(user.getId()).orElse(null);
		UserSummary summary = UserSummary.of(user,
				reputation == null ? 80 : reputation.getCreditScore(),
				reputation == null ? java.math.BigDecimal.ZERO : reputation.getAvgRating());
		return new TokenResponse(accessToken, rawRefresh, jwtService.accessTokenSeconds(), summary);
	}

	private String randomToken() {
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		return HexFormat.of().formatHex(bytes);
	}

	private String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 不可用", ex);
		}
	}
}
