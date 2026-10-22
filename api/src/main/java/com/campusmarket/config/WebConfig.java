package com.campusmarket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final CorsProperties corsProperties;
	private final UploadProperties uploadProperties;

	public WebConfig(CorsProperties corsProperties, UploadProperties uploadProperties) {
		this.corsProperties = corsProperties;
		this.uploadProperties = uploadProperties;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
				.allowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new))
				.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.exposedHeaders("X-Trace-Id")
				.allowCredentials(true)
				.maxAge(3600);
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String location = Path.of(uploadProperties.dir()).toAbsolutePath().normalize().toUri().toString();
		registry.addResourceHandler(uploadProperties.publicPath() + "/**")
				.addResourceLocations(location);
	}
}
