package com.campusmarket.auth;

import com.campusmarket.common.BusinessException;
import com.campusmarket.common.ErrorCode;
import com.campusmarket.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService {

	private final JwtProperties properties;
	private final SecretKey key;

	public JwtService(JwtProperties properties) {
		this.properties = properties;
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public String generateAccessToken(Long userId, String role, boolean verified) {
		Instant now = Instant.now();
		Instant expiry = now.plusSeconds(properties.accessTokenMinutes() * 60);
		return Jwts.builder()
				.subject(String.valueOf(userId))
				.claim("role", role)
				.claim("verified", verified)
				.issuer(properties.issuer())
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiry))
				.signWith(key)
				.compact();
	}

	public long accessTokenSeconds() {
		return properties.accessTokenMinutes() * 60;
	}

	public long refreshTokenDays() {
		return properties.refreshTokenDays();
	}

	public Long parseUserId(String token) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(key)
					.requireIssuer(properties.issuer())
					.build()
					.parseSignedClaims(token)
					.getPayload();
			return Long.valueOf(claims.getSubject());
		}
		catch (ExpiredJwtException ex) {
			throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "登录已过期，请重新登录");
		}
		catch (JwtException | IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.TOKEN_INVALID, "无效的访问令牌");
		}
	}
}
