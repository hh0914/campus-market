package com.campusmarket.auth;

import com.campusmarket.common.BusinessException;
import com.campusmarket.user.Role;
import com.campusmarket.user.User;
import com.campusmarket.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 解析 Bearer Token 并实时读取用户状态，避免封禁后旧令牌仍然有效。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserRepository userRepository;

	public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
		this.jwtService = jwtService;
		this.userRepository = userRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith("Bearer ")) {
			String token = header.substring(7).trim();
			try {
				Long userId = jwtService.parseUserId(token);
				userRepository.findById(userId)
						.filter(User::isActive)
						.ifPresent(this::authenticate);
			}
			catch (BusinessException ignored) {
				SecurityContextHolder.clearContext();
			}
		}
		chain.doFilter(request, response);
	}

	private void authenticate(User user) {
		AuthUser principal = new AuthUser(user.getId(), user.getRole(), user.isVerified());
		List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
		UsernamePasswordAuthenticationToken authentication =
				new UsernamePasswordAuthenticationToken(principal, null, authorities);
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	public static Role roleOf(AuthUser user) {
		return user == null ? null : user.role();
	}
}
