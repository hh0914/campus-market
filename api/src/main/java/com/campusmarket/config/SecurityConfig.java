package com.campusmarket.config;

import com.campusmarket.auth.JwtAuthenticationFilter;
import com.campusmarket.common.ApiResponse;
import com.campusmarket.common.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	public static final String[] PUBLIC_ANY = {
			"/api/v1/health",
			"/api/v1/auth/**",
			"/api/v1/files/**",
			"/v3/api-docs/**",
			"/swagger-ui/**",
			"/swagger-ui.html",
			"/actuator/health",
			"/actuator/info"
	};

	public static final String[] PUBLIC_GET = {
			"/api/v1/items/**",
			"/api/v1/categories/**",
			"/api/v1/users/*/profile"
	};

	private final ObjectMapper objectMapper;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	public SecurityConfig(ObjectMapper objectMapper, JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.objectMapper = objectMapper;
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

	@Bean
	public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
		FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setEnabled(false);
		return registration;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.cors(Customizer.withDefaults())
			.httpBasic(hbc -> hbc.disable())
			.formLogin(form -> form.disable())
			.logout(logout -> logout.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.authorizeHttpRequests(auth -> auth
					.requestMatchers(PUBLIC_ANY).permitAll()
					.requestMatchers(org.springframework.http.HttpMethod.GET, PUBLIC_GET).permitAll()
					.requestMatchers("/api/v1/admin/**").hasAnyRole("MODERATOR", "ADMIN")
					.anyRequest().authenticated())
			.exceptionHandling(handler -> handler
					.authenticationEntryPoint(authenticationEntryPoint())
					.accessDeniedHandler(accessDeniedHandler()));
		return http.build();
	}

	private AuthenticationEntryPoint authenticationEntryPoint() {
		return (request, response, ex) -> write(response, HttpServletResponse.SC_UNAUTHORIZED,
				ErrorCode.UNAUTHORIZED, "未登录或登录已过期");
	}

	private AccessDeniedHandler accessDeniedHandler() {
		return (request, response, ex) -> write(response, HttpServletResponse.SC_FORBIDDEN,
				ErrorCode.FORBIDDEN, "没有操作权限");
	}

	private void write(HttpServletResponse response, int status, ErrorCode code, String message) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getWriter(), ApiResponse.error(code, message));
	}
}
