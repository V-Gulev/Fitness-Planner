package bg.fitnessplanner;

import bg.fitnessplanner.account.persistence.UserAccount;
import bg.fitnessplanner.account.persistence.UserAccountRepository;
import jakarta.persistence.EntityManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Instant;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("mysql-test")
@Import({FoundationMySqlIT.FailureEndpoint.class, FoundationMySqlIT.ErrorTestSecurity.class})
class FoundationMySqlIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Flyway flyway;

    @Autowired
    private UserAccountRepository accounts;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TestRestTemplate http;

    @Test
    void usesTheDedicatedTestDatabaseAndAppliesTheMigrationExactlyOnce() {
        assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class))
                .isEqualTo("fitness_planner_test");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = 1",
                Integer.class)).isEqualTo(1);
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test
    @Transactional
    void roundTripsTheAccountMappingWithMicrosecondTimestampPrecision() {
        Instant createdAt = Instant.parse("2026-11-06T09:00:00.123456Z");
        UserAccount saved = accounts.saveAndFlush(new UserAccount(
                uniqueEmail(), "encoded-password-fixture-not-a-real-credential", createdAt));
        entityManager.clear();

        UserAccount reloaded = accounts.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getEmailNormalized()).isEqualTo(saved.getEmailNormalized());
        assertThat(reloaded.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @Transactional
    void databaseRejectsDuplicateNormalizedEmail() {
        String email = uniqueEmail();
        insertAccount(email, "encoded-fixture");

        assertThatThrownBy(() -> insertAccount(email, "another-encoded-fixture"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void databaseRejectsAnEmptyPasswordHash() {
        assertThatThrownBy(() -> insertAccount(uniqueEmail(), "   "))
                .isInstanceOf(DataAccessException.class)
                .hasCauseInstanceOf(SQLException.class)
                .satisfies(failure -> {
                    SQLException sql = (SQLException) failure.getCause();
                    assertThat(sql.getErrorCode()).isEqualTo(3819);
                    assertThat(sql.getMessage()).contains("ck_app_user_password_nonempty");
                });
    }

    @Test
    void rendersHomeOverRealHttpWithDatabaseEnabled() {
        ResponseEntity<String> response = get("/", MediaType.TEXT_HTML);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("A stronger week", "Log actual sets and review explained load suggestions");
    }

    @Test
    void rendersTheCustomNotFoundPageThroughTheServletErrorDispatch() {
        ResponseEntity<String> response = get("/missing-page", MediaType.TEXT_HTML);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("This page isn’t here.", "Return to home")
                .doesNotContain("Whitelabel Error Page", "NoResourceFoundException");
    }

    @Test
    void unexpectedFailureDoesNotExposeExceptionDetailsInHtml() {
        ResponseEntity<String> response = get("/__test/failure", MediaType.TEXT_HTML);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("Something went wrong.")
                .doesNotContain("internal-exception-detail", "IllegalStateException", "stackTrace");
    }

    @Test
    void unexpectedFailureDoesNotExposeExceptionDetailsInJson() {
        ResponseEntity<String> response = get("/__test/failure?trace=true&message=true", MediaType.APPLICATION_JSON);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("\"status\":500")
                .doesNotContain("internal-exception-detail", "IllegalStateException", "\"trace\"", "\"message\"");
    }

    private ResponseEntity<String> get(String path, MediaType accept) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(accept));
        return http.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private void insertAccount(String email, String passwordHash) {
        jdbc.update("INSERT INTO app_user (email_normalized, password_hash, created_at) VALUES (?, ?, UTC_TIMESTAMP(6))",
                email, passwordHash);
    }

    private String uniqueEmail() {
        return UUID.randomUUID() + "@example.test";
    }

    /** Deliberately failing endpoint exists only in the integration-test context. */
    @TestComponent
    @Controller
    static class FailureEndpoint {

        @GetMapping("/__test/failure")
        String fail() {
            throw new IllegalStateException("internal-exception-detail");
        }
    }

    /** Allows the test-only URLs to exercise real servlet error dispatch without a login flow. */
    @TestConfiguration(proxyBeanMethods = false)
    static class ErrorTestSecurity {
        @Bean
        @Order(0)
        SecurityFilterChain testErrorEndpoints(HttpSecurity http) throws Exception {
            http.securityMatcher("/__test/failure", "/missing-page")
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
            return http.build();
        }
    }
}
