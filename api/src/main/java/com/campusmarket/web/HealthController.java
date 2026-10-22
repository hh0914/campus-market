package com.campusmarket.web;

import com.campusmarket.common.ApiResponse;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

	private final JdbcTemplate jdbcTemplate;
	private final Environment environment;

	public HealthController(JdbcTemplate jdbcTemplate, Environment environment) {
		this.jdbcTemplate = jdbcTemplate;
		this.environment = environment;
	}

	@GetMapping("/health")
	public ApiResponse<Map<String, Object>> health() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", "UP");
		body.put("app", environment.getProperty("spring.application.name"));
		body.put("profiles", String.join(",", environment.getActiveProfiles()));
		body.put("database", databaseStatus());
		body.put("time", OffsetDateTime.now().toString());
		return ApiResponse.ok(body);
	}

	private String databaseStatus() {
		try {
			Integer value = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
			return value != null && value == 1 ? "UP" : "DOWN";
		}
		catch (RuntimeException ex) {
			return "DOWN";
		}
	}
}
