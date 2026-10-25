package com.ubique.prayer;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
class PostgresTestConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer("postgres:18.6-bookworm")
			.withDatabaseName("ubique_prayer_test")
			.withUsername("test")
			.withPassword("test");
	}

}
