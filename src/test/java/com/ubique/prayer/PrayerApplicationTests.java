package com.ubique.prayer;

import java.sql.Connection;
import javax.sql.DataSource;

import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
	"spring.config.import=",
	"spring.datasource.url=",
	"spring.datasource.username=",
	"spring.datasource.password=",
	"springdoc.api-docs.enabled=true",
	"springdoc.swagger-ui.enabled=true"
})
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class PrayerApplicationTests {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private PostgreSQLContainer postgres;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private Flyway flyway;

	@Autowired
	private MockMvc mvc;

	@Autowired
	private Validator validator;

	@Test
	void connectsToIsolatedPostgres() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			assertThat(connection.getMetaData().getURL())
				.contains(":" + postgres.getMappedPort(5432) + "/ubique_prayer_test");
		}
		assertThat(jdbc.queryForObject("SELECT current_database()", String.class))
			.isEqualTo("ubique_prayer_test");
	}

	@Test
	void flywayInitializesTheTestDatabase() throws Exception {
		try (Connection connection = flyway.getConfiguration().getDataSource().getConnection()) {
			assertThat(connection.getCatalog()).isEqualTo("ubique_prayer_test");
		}
		assertThat(jdbc.queryForObject("SELECT to_regclass('public.flyway_schema_history')::text", String.class))
			.isEqualTo("flyway_schema_history");
		assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
	}

	@Test
	void healthIsAvailableWithoutAuthentication() throws Exception {
		mvc.perform(get("/actuator/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void openApiDocumentIsGenerated() throws Exception {
		mvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.openapi").exists())
			.andExpect(jsonPath("$.info.title").value("Ubique Prayer API"));
	}

	@Test
	void swaggerUiAssetsAreAvailableWithoutAuthentication() throws Exception {
		mvc.perform(get("/swagger-ui/index.html"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Swagger UI")));
	}

	@Test
	void anonymousApiRequestsAreRejectedWithoutLoginRedirect() throws Exception {
		mvc.perform(get("/api/private/probe"))
			.andExpect(status().isUnauthorized())
			.andExpect(header().doesNotExist("Location"));
	}

	@Test
	void bootstrapPolicyDoesNotGrantAccessToBusinessApis() throws Exception {
		mvc.perform(get("/api/private/probe").with(user("test-user")))
			.andExpect(status().isForbidden());
	}

	@Test
	void beanValidationAcceptsValidInputAndRejectsInvalidInput() {
		assertThat(validator.validate(new ValidationInput("invalid-email"))).isNotEmpty();
		assertThat(validator.validate(new ValidationInput(""))).isNotEmpty();
		assertThat(validator.validate(new ValidationInput("developer@example.com"))).isEmpty();
	}

	private record ValidationInput(@NotBlank @Email String email) {
	}

}
