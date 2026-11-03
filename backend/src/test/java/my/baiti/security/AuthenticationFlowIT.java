package my.baiti.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.http.Cookie;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import my.baiti.testing.AuthenticationFixture;
import my.baiti.testing.NativePostgresTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationFlowIT extends NativePostgresTestSupport {

  private static final UUID A = UUID.fromString("ad8cdb3d-885a-4f0d-b678-2331ce582805");
  private static final String PASSWORD = "synthetic-login-password";
  private static final String ORIGIN = "https://synthetic-client.example.invalid";
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final AtomicInteger ADDRESSES = new AtomicInteger();
  @Autowired private MockMvc mvc;
  @Autowired private DataSource source;

  @Autowired
  @Qualifier("platformConnectionPool")
  private HikariDataSource platformPool;

  @Autowired
  @Qualifier("platformJdbcTemplate")
  private JdbcTemplate platform;

  private AuthenticationFixture fixture;
  private String clientAddress;

  @BeforeEach
  void createOnlyThisTestsSyntheticUser() {
    fixture = new AuthenticationFixture(source, A, PASSWORD);
    clientAddress = "192.0.2." + ADDRESSES.incrementAndGet();
  }

  @AfterEach
  void removeOnlyThisTestsSyntheticRecords() {
    if (fixture != null) {
      fixture.close();
    }
  }

  @Test
  void verifiedPasswordIssuesAccessAndHttpOnlyRefreshCookie() throws Exception {
    var result = login();
    assertThat(result.getResponse().getHeader("Set-Cookie"))
        .contains(
            "HttpOnly", "Secure", "SameSite=Strict", "Path=/api/auth/refresh", "Max-Age=604800");
    assertThat(result.getResponse().getHeader("Cache-Control")).contains("no-store");
    String raw = refreshCookie(result).getValue();
    assertThat(raw).matches("[A-Za-z0-9_-]{43}");
    fixture.scoped(
        jdbc -> {
          assertThat(
                  jdbc.queryForObject(
                      "SELECT token_hash FROM refresh_tokens WHERE user_id = ?",
                      String.class,
                      fixture.userId()))
              .isEqualTo(RefreshTokenService.hash(raw))
              .isNotEqualTo(raw);
          assertThat(
                  jdbc.queryForObject(
                      "SELECT last_login_at IS NOT NULL FROM users WHERE id = ?",
                      Boolean.class,
                      fixture.userId()))
              .isTrue();
        });
  }

  @Test
  void refreshRotationRevokesTheWholeFamilyWhenAnOldTokenIsReused() throws Exception {
    Cookie original = refreshCookie(login());
    Cookie replacement =
        refreshCookie(mvc.perform(refresh(original)).andExpect(status().isOk()).andReturn());
    assertThat(replacement.getValue()).isNotEqualTo(original.getValue());
    mvc.perform(refresh(original)).andExpect(status().isUnauthorized());
    mvc.perform(refresh(replacement)).andExpect(status().isUnauthorized());
    fixture.scoped(
        jdbc ->
            assertThat(
                    jdbc.queryForObject(
                        "SELECT count(*) FROM refresh_tokens WHERE user_id = ? AND revoked_at IS NULL",
                        Integer.class,
                        fixture.userId()))
                .isZero());
  }

  @Test
  void wrongPasswordUnknownUserInactiveUserAndUnknownSlugHaveTheSame401Body() throws Exception {
    String wrong =
        mvc.perform(loginRequest(fixture.email(), "synthetic-wrong-password"))
            .andExpect(status().isUnauthorized())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String unknown =
        mvc.perform(loginRequest("synthetic-unknown@example.invalid", PASSWORD))
            .andExpect(status().isUnauthorized())
            .andReturn()
            .getResponse()
            .getContentAsString();
    fixture.scoped(
        jdbc -> jdbc.update("UPDATE users SET status = 'INACTIVE' WHERE id = ?", fixture.userId()));
    String inactive =
        mvc.perform(loginRequest(fixture.email(), PASSWORD))
            .andExpect(status().isUnauthorized())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String unknownSlug =
        mvc.perform(
                post("/api/auth/login")
                    .with(client())
                    .header("X-Tenant-Slug", "synthetic-unknown-community")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        JSON.writeValueAsString(
                            Map.of("email", fixture.email(), "password", PASSWORD))))
            .andExpect(status().isUnauthorized())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(wrong).isEqualTo(unknown).isEqualTo(inactive).isEqualTo(unknownSlug);
    fixture.scoped(
        jdbc ->
            assertThat(
                    jdbc.queryForObject(
                        "SELECT count(*) FROM refresh_tokens WHERE user_id = ?",
                        Integer.class,
                        fixture.userId()))
                .isZero());
  }

  @Test
  void cookieRefreshRequiresAllowedOriginAndExplicitHeaderWithoutConsumingTheToken()
      throws Exception {
    Cookie token = refreshCookie(login());
    mvc.perform(request("/api/auth/refresh").cookie(token)).andExpect(status().isForbidden());
    mvc.perform(request("/api/auth/refresh").header("Origin", ORIGIN).cookie(token))
        .andExpect(status().isForbidden());
    mvc.perform(
            request("/api/auth/refresh")
                .header("Origin", "https://synthetic-evil.example.invalid")
                .header("X-BAiTI-Refresh", "true")
                .cookie(token))
        .andExpect(status().isForbidden());
    mvc.perform(refresh(token)).andExpect(status().isOk());
  }

  @Test
  void bodyFallbackWithoutAmbientCookieRotatesTheToken() throws Exception {
    Cookie token = refreshCookie(login());
    mvc.perform(
            request("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("token", token.getValue()))))
        .andExpect(status().isOk())
        .andExpect(cookie().exists("baiti_refresh"));
  }

  @Test
  void expiredAndForeignTenantRefreshCannotIssueNewTokens() throws Exception {
    Cookie token = refreshCookie(login());
    mvc.perform(
            post("/api/auth/refresh")
                .with(client())
                .header("X-Tenant-Slug", "baiti-foundation-b")
                .header("Origin", ORIGIN)
                .header("X-BAiTI-Refresh", "true")
                .cookie(token))
        .andExpect(status().isUnauthorized());
    fixture.scoped(
        jdbc ->
            jdbc.update(
                "UPDATE refresh_tokens SET expires_at = ? WHERE user_id = ?",
                Timestamp.from(Instant.now().minusSeconds(1)),
                fixture.userId()));
    mvc.perform(refresh(token)).andExpect(status().isUnauthorized());
    fixture.scoped(
        jdbc ->
            assertThat(
                    jdbc.queryForObject(
                        "SELECT count(*) FROM refresh_tokens WHERE user_id = ?",
                        Integer.class,
                        fixture.userId()))
                .isEqualTo(1));
  }

  @Test
  void meRequiresBearerAndLogoutRevokesEveryRefreshFamilyForTheUser() throws Exception {
    var first = login();
    var second = login();
    String access =
        JSON.readTree(first.getResponse().getContentAsString()).get("accessToken").asString();
    mvc.perform(
            get("/api/auth/me")
                .with(client())
                .header("X-Tenant-Slug", "baiti-foundation-a")
                .header("Authorization", "Bearer " + access))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(fixture.userId().toString()))
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
    mvc.perform(get("/api/auth/me").with(client()).header("X-Tenant-Slug", "baiti-foundation-a"))
        .andExpect(status().isUnauthorized());
    mvc.perform(request("/api/auth/logout").header("Authorization", "Bearer " + access))
        .andExpect(status().isNoContent())
        .andExpect(cookie().maxAge("baiti_refresh", 0));
    mvc.perform(refresh(refreshCookie(first))).andExpect(status().isUnauthorized());
    mvc.perform(refresh(refreshCookie(second))).andExpect(status().isUnauthorized());
  }

  @Test
  void concurrentRotationHasOneWinnerAndReplayRevokesItsReplacement() throws Exception {
    Cookie token = refreshCookie(login());
    var start = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);
    try {
      java.util.concurrent.Callable<MvcResult> operation =
          () -> {
            start.await();
            return mvc.perform(refresh(token)).andReturn();
          };
      var one = executor.submit(operation);
      var two = executor.submit(operation);
      start.countDown();
      var results = java.util.List.of(one.get(20, TimeUnit.SECONDS), two.get(20, TimeUnit.SECONDS));
      assertThat(results)
          .extracting(result -> result.getResponse().getStatus())
          .containsExactlyInAnyOrder(200, 401);
      var winner =
          results.stream()
              .filter(result -> result.getResponse().getStatus() == 200)
              .findFirst()
              .orElseThrow();
      mvc.perform(refresh(refreshCookie(winner))).andExpect(status().isUnauthorized());
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void credentialedCorsAllowsOnlyTheConfiguredOrigin() throws Exception {
    mvc.perform(
            options("/api/auth/login")
                .header("Origin", ORIGIN)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,x-tenant-slug"))
        .andExpect(status().isOk());
    mvc.perform(
            options("/api/auth/login")
                .header("Origin", "https://synthetic-evil.example.invalid")
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isForbidden());
  }

  @Test
  void loginRateLimitRejectsTheSixthAttempt() throws Exception {
    for (int attempt = 0; attempt < 5; attempt++) {
      mvc.perform(loginRequest(fixture.email(), "synthetic-wrong-password"))
          .andExpect(status().isUnauthorized());
    }
    mvc.perform(loginRequest(fixture.email(), "synthetic-wrong-password"))
        .andExpect(status().isTooManyRequests());
  }

  private MvcResult login() throws Exception {
    return mvc.perform(loginRequest(fixture.email(), PASSWORD))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isString())
        .andExpect(jsonPath("$.user.email").value(fixture.email()))
        .andExpect(jsonPath("$.refreshToken").doesNotExist())
        .andExpect(cookie().httpOnly("baiti_refresh", true))
        .andExpect(cookie().secure("baiti_refresh", true))
        .andReturn();
  }

  @Test
  void invalidCredentialsProduceProblemDetailsWithoutEchoingTheirValues() throws Exception {
    var result =
        mvc.perform(loginRequest("synthetic-invalid-email", PASSWORD))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors[0].field").value("email"))
            .andReturn();
    assertThat(result.getResponse().getContentType()).startsWith("application/problem+json");
    assertThat(result.getResponse().getContentAsString())
        .doesNotContain(PASSWORD, "synthetic-invalid-email");
  }

  @Test
  void suspendedCommunityReturnsGeneric401AndTheFixtureChangeRollsBack() throws Exception {
    new TransactionTemplate(new DataSourceTransactionManager(platformPool))
        .executeWithoutResult(
            transaction -> {
              transaction.setRollbackOnly();
              platform.update("UPDATE tenants SET status = 'SUSPENDED' WHERE id = ?", A);
              try {
                mvc.perform(loginRequest(fixture.email(), PASSWORD))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Authentication failed"));
              } catch (Exception failure) {
                throw new IllegalStateException(
                    "Synthetic suspended-community request failed", failure);
              }
            });
    login();
  }

  @Test
  void inactiveUserCannotRefreshAndEveryOutstandingFamilyIsRevoked() throws Exception {
    Cookie token = refreshCookie(login());
    login();
    fixture.scoped(
        jdbc -> jdbc.update("UPDATE users SET status = 'INACTIVE' WHERE id = ?", fixture.userId()));
    mvc.perform(refresh(token)).andExpect(status().isUnauthorized());
    fixture.scoped(
        jdbc ->
            assertThat(
                    jdbc.queryForObject(
                        "SELECT count(*) FROM refresh_tokens WHERE user_id = ? AND revoked_at IS NULL",
                        Integer.class,
                        fixture.userId()))
                .isZero());
  }

  @Test
  void multibytePasswordsCannotExceedBcryptsByteLimit() throws Exception {
    mvc.perform(loginRequest(fixture.email(), "界".repeat(25))).andExpect(status().isUnauthorized());
  }

  @Test
  void oversizedAuthenticationBodyIsRejectedBeforeJsonParsing() throws Exception {
    mvc.perform(
            request("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    JSON.writeValueAsString(
                        Map.of(
                            "email",
                            fixture.email(),
                            "password",
                            PASSWORD,
                            "syntheticPadding",
                            "x".repeat(9000)))))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(413));
  }

  @Test
  void accountRateLimitAppliesAcrossDifferentClientAddresses() throws Exception {
    for (int attempt = 0; attempt < 5; attempt++) {
      String address = "198.51.100." + (100 + attempt);
      mvc.perform(
              loginRequest(fixture.email(), "synthetic-wrong-password")
                  .with(
                      request -> {
                        request.setRemoteAddr(address);
                        return request;
                      }))
          .andExpect(status().isUnauthorized());
    }
    mvc.perform(
            loginRequest(fixture.email(), "synthetic-wrong-password")
                .with(
                    request -> {
                      request.setRemoteAddr("198.51.100.200");
                      return request;
                    }))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  void unknownSlugRequestsAreStillLimitedBeforeResolution() throws Exception {
    for (int attempt = 0; attempt < 5; attempt++) {
      mvc.perform(
              post("/api/auth/login")
                  .with(client())
                  .header("X-Tenant-Slug", "synthetic-unknown-" + attempt)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      JSON.writeValueAsString(
                          Map.of("email", fixture.email(), "password", PASSWORD))))
          .andExpect(status().isUnauthorized());
    }
    mvc.perform(
            post("/api/auth/login")
                .with(client())
                .header("X-Tenant-Slug", "synthetic-unknown-last")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    JSON.writeValueAsString(
                        Map.of("email", fixture.email(), "password", PASSWORD))))
        .andExpect(status().isTooManyRequests());
  }

  private MockHttpServletRequestBuilder loginRequest(String email, String password)
      throws Exception {
    return request("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(JSON.writeValueAsString(Map.of("email", email, "password", password)));
  }

  private MockHttpServletRequestBuilder refresh(Cookie cookie) {
    return request("/api/auth/refresh")
        .header("Origin", ORIGIN)
        .header("X-BAiTI-Refresh", "true")
        .cookie(cookie);
  }

  private MockHttpServletRequestBuilder request(String path) {
    return post(path).with(client()).header("X-Tenant-Slug", "baiti-foundation-a");
  }

  private RequestPostProcessor client() {
    return request -> {
      request.setRemoteAddr(clientAddress);
      return request;
    };
  }

  private static Cookie refreshCookie(MvcResult result) {
    Cookie cookie = result.getResponse().getCookie("baiti_refresh");
    assertThat(cookie).isNotNull();
    return cookie;
  }
}
