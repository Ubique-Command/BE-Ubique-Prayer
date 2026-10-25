package com.ubique.prayer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

	@Bean
	OpenAPI prayerOpenApi() {
		return new OpenAPI().info(new Info()
			.title("Ubique Prayer API")
			.version("v1")
			.description("Ubique Prayer 백엔드 API 문서"));
	}

}
