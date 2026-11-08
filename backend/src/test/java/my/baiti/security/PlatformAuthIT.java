package my.baiti.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import my.baiti.people.DefaultRoleGrants;
import my.baiti.tenancy.admin.TenantOnboardingService;
import my.baiti.tenancy.admin.TenantRegistration;
import my.baiti.testing.NativePostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlatformAuthIT extends NativePostgresTestSupport {
  @Autowired private MockMvc mvc;
  @Autowired private JwtService jwt;

  @Autowired
  @Qualifier("platformJdbcTemplate")
  private JdbcTemplate platform;

  @Test
  void authenticatedPlatformOperatorCreatesTenantWithCompleteSeeds() {
    transaction()
        .executeWithoutResult(
            status -> {
              status.setRollbackOnly();
              var id = insertOperator();
              var slug = "onboard-" + id;
              var token = jwt.issuePlatform(new PlatformPrincipal(id));
              try {
                mvc.perform(create(slug, token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tenantId").isString())
                    .andExpect(jsonPath("$.adminUserId").isString())
                    .andExpect(jsonPath("$.initialPassword").doesNotExist());
                assertThat(
                        platform.queryForObject("SELECT public.baiti_current_tenant()", UUID.class))
                    .isNull();
                mvc.perform(create(slug, token)).andExpect(status().isConflict());
                var tenant =
                    platform.queryForObject(
                        "SELECT id FROM tenants WHERE slug = CAST(? AS citext)", UUID.class, slug);
                bind(tenant);
                assertThat(
                        platform.queryForList(
                            "SELECT name FROM roles WHERE tenant_id = ?", String.class, tenant))
                    .containsExactlyInAnyOrder("Admin", "Manager", "Resident", "Guard");
                assertThat(count("permissions", tenant)).isEqualTo(42);
                assertThat(count("roles_permissions", tenant))
                    .isEqualTo(DefaultRoleGrants.all().values().stream().mapToInt(Set::size).sum());
                assertThat(
                        platform.queryForObject(
                            "SELECT must_change_password FROM users WHERE tenant_id = ?",
                            Boolean.class,
                            tenant))
                    .isTrue();
                assertThat(
                        new BCryptPasswordEncoder()
                            .matches(
                                "synthetic initial phrase",
                                platform.queryForObject(
                                    "SELECT password_hash FROM users WHERE tenant_id = ?",
                                    String.class,
                                    tenant)))
                    .isTrue();
                assertThat(
                        platform.queryForObject(
                            "SELECT action FROM activity_logs WHERE tenant_id = ?",
                            String.class,
                            tenant))
                    .isEqualTo("TENANT_CREATED");
                assertThat(
                        platform.queryForObject(
                            "SELECT detail->>'platformAdminId' FROM activity_logs WHERE tenant_id = ?",
                            String.class,
                            tenant))
                    .isEqualTo(id.toString());
              } catch (Exception failure) {
                throw new AssertionError("Platform tenant creation failed", failure);
              }
            });
  }

  @Test
  void i9StepFourFailureRollsBackTenantAndEverySeed() {
    var slug = "rollback-" + UUID.randomUUID();
    var candidate = new AtomicReference<UUID>();
    var encoder =
        new PasswordEncoder() {
          @Override
          public String encode(CharSequence rawPassword) {
            candidate.set(
                platform.queryForObject("SELECT public.baiti_current_tenant()", UUID.class));
            assertThat(candidate.get()).isNotNull();
            assertThat(count("roles", candidate.get())).isEqualTo(4);
            assertThat(count("permissions", candidate.get())).isEqualTo(42);
            throw new IllegalStateException("synthetic step-four failure");
          }

          @Override
          public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return false;
          }
        };
    var service = new TenantOnboardingService(platform, encoder);
    var registration =
        new TenantRegistration(
            slug,
            "Synthetic Community",
            "initial@example.invalid",
            "Synthetic Admin",
            "synthetic initial phrase");
    assertThatThrownBy(
            () ->
                service.create(new PlatformPrincipal(UUID.randomUUID()), registration, "127.0.0.1"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("synthetic step-four failure");
    assertThat(
            platform.queryForObject(
                "SELECT count(*) FROM tenants WHERE slug = CAST(? AS citext)", Integer.class, slug))
        .isZero();
    transaction()
        .executeWithoutResult(
            status -> {
              status.setRollbackOnly();
              assertThat(
                      platform.queryForObject("SELECT public.baiti_current_tenant()", UUID.class))
                  .isNull();
              bind(candidate.get());
              for (var table :
                  Set.of("roles", "permissions", "roles_permissions", "users", "activity_logs")) {
                assertThat(count(table, candidate.get())).as(table).isZero();
              }
            });
  }

  @Test
  void tenantAdminTokenCannotListOrCreateCommunities() throws Exception {
    var token =
        jwt.issue(
            new TenantPrincipal(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Set.of("Admin"),
                Set.of(PlatformPrincipal.AUTHORITY, "TENANT_CREATE")));
    mvc.perform(get("/api/admin/tenants").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
    mvc.perform(create("denied-" + UUID.randomUUID(), token)).andExpect(status().isForbidden());
    mvc.perform(get("/api/admin/tenants")).andExpect(status().isUnauthorized());
  }

  @Test
  void activePlatformAccountCanSignInWithoutTenantResolution() {
    transaction()
        .executeWithoutResult(
            status -> {
              status.setRollbackOnly();
              var id = insertOperator();
              var email = id + "@example.invalid";
              try {
                var response =
                    mvc.perform(
                            post("/api/admin/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                    "{\"email\":\""
                                        + email
                                        + "\",\"password\":\"synthetic fixture phrase\"}"))
                        .andExpect(status().isOk())
                        .andExpect(header().string("Cache-Control", "no-store"))
                        .andExpect(header().doesNotExist("Set-Cookie"))
                        .andExpect(jsonPath("$.accessToken").isString())
                        .andExpect(jsonPath("$.expiresIn").value(900))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
                var token =
                    JsonMapper.builder().build().readTree(response).get("accessToken").asString();
                assertThat(jwt.verifyPlatform(token).adminId()).isEqualTo(id);
                mvc.perform(
                        get("/api/admin/tenants?size=200")
                            .header("Authorization", "Bearer " + token)
                            .header("X-Tenant-Slug", "nonexistent-tenant"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.size").value(100));
                mvc.perform(
                        get("/api/auth/me")
                            .header("Authorization", "Bearer " + token)
                            .header("X-Tenant-Slug", "baiti-foundation-a"))
                    .andExpect(status().isUnauthorized());
                assertThat(
                        platform.queryForObject(
                            "SELECT last_login_at IS NOT NULL FROM super_admins WHERE id = ?",
                            Boolean.class,
                            id))
                    .isTrue();
                platform.update("UPDATE super_admins SET status = 'SUSPENDED' WHERE id = ?", id);
                mvc.perform(get("/api/admin/tenants").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
              } catch (Exception failure) {
                throw new AssertionError("Platform sign-in failed", failure);
              }
            });
  }

  private TransactionTemplate transaction() {
    return new TransactionTemplate(new JdbcTransactionManager(platform.getDataSource()));
  }

  private UUID insertOperator() {
    var id = UUID.randomUUID();
    platform.update(
        "INSERT INTO super_admins (id, email, name, password_hash) VALUES (?, ?, ?, ?)",
        id,
        id + "@example.invalid",
        "Synthetic platform operator",
        new BCryptPasswordEncoder(4).encode("synthetic fixture phrase"));
    return id;
  }

  private void bind(UUID tenant) {
    platform.queryForObject(
        "SELECT set_config('app.current_tenant', ?, true)", String.class, tenant.toString());
  }

  private Integer count(String table, UUID tenant) {
    return platform.queryForObject(
        "SELECT count(*) FROM " + table + " WHERE tenant_id = ?", Integer.class, tenant);
  }

  private static MockHttpServletRequestBuilder create(String slug, String token) {
    return post("/api/admin/tenants")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            "{\"slug\":\""
                + slug
                + "\",\"name\":\"Synthetic Community\","
                + "\"adminEmail\":\"initial@example.invalid\",\"adminName\":\"Synthetic Admin\","
                + "\"initialPassword\":\"synthetic initial phrase\"}");
  }
}
