package my.baiti.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.sql.DataSource;
import my.baiti.common.ResourceNotFoundException;
import my.baiti.property.BlockRepository;
import my.baiti.property.Unit;
import my.baiti.property.UnitRepository;
import my.baiti.security.JwtService;
import my.baiti.security.PlatformPrincipal;
import my.baiti.security.TenantPrincipal;
import my.baiti.tenancy.TenantContext;
import my.baiti.tenancy.admin.TenantOnboardingService;
import my.baiti.tenancy.admin.TenantRegistration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "audit-test"})
@Import(TenantIsolationIT.UnitProbe.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TenantIsolationIT extends NativePostgresTestSupport {
  private static final String PROBE = "/api/test/isolation/units";
  private static final String ADMIN_EMAIL = "isolation-admin@example.invalid";
  private static final String AUDIT_TEST_PASSWORD = "synthetic audit phrase";

  @Autowired private MockMvc mvc;
  @Autowired private JwtService jwt;
  @Autowired private TenantOnboardingService onboarding;
  @Autowired private DataSource appSource;

  @Autowired
  @Qualifier("appJdbcTemplate")
  private JdbcTemplate app;

  @Autowired
  @Qualifier("platformJdbcTemplate")
  private JdbcTemplate platform;

  @Autowired
  @Qualifier("tenantAuditExecutor")
  private ThreadPoolTaskExecutor auditExecutor;

  private Community a;
  private Community b;

  @BeforeAll
  void createOnboardedCommunities() {
    var marker = NativePostgresConfig.from(System.getenv()).ownershipMarker();
    a = fixture(marker, "a");
    b = fixture(marker, "b");
  }

  @AfterAll
  void removePerRunPropertyFixtures() {
    removeProperties(a);
    removeProperties(b);
  }

  @Test
  void i1UnitListingContainsOnlyTheCallingTenantsUnits() throws Exception {
    mvc.perform(as(a, get(PROBE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0]").value(a.unit().toString()));
    mvc.perform(as(b, get(PROBE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0]").value(b.unit().toString()));
  }

  @Test
  void i2KnownForeignUnitAndUnknownUnitHaveTheSameNeutral404() throws Exception {
    mvc.perform(as(a, get(PROBE + "/{unit}", a.unit())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(a.unit().toString()));
    for (UUID id : List.of(b.unit(), UUID.randomUUID())) {
      mvc.perform(as(a, get(PROBE + "/{unit}", id)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.detail").value("Resource not found"));
    }
  }

  @Test
  void i3ForeignBlockReferenceIsRejectedBeforeWritingAUnit() throws Exception {
    var probeId = UUID.randomUUID();
    try {
      mvc.perform(as(a, post(PROBE + "/{block}", b.block()))).andExpect(status().isNotFound());
      mvc.perform(as(a, post(PROBE + "/{block}", a.block()))).andExpect(status().isCreated());
      scoped(
          a.id(),
          () -> {
            assertThatThrownBy(
                    () ->
                        app.update(
                            "INSERT INTO units (id, tenant_id, block_id, unit_no) VALUES (?, ?, ?, ?)",
                            probeId,
                            a.id(),
                            b.block(),
                            probeId.toString()))
                .isInstanceOfSatisfying(
                    DataAccessException.class,
                    failure ->
                        assertThat(((SQLException) failure.getMostSpecificCause()).getSQLState())
                            .isEqualTo("23503"));
            assertThat(
                    app.queryForObject(
                        "SELECT count(*) FROM units WHERE block_id = ?", Long.class, b.block()))
                .isZero();
            assertThat(
                    app.queryForObject(
                        "SELECT count(*) FROM units WHERE block_id = ?", Long.class, a.block()))
                .isEqualTo(2);
          });
    } finally {
      scoped(
          a.id(),
          () ->
              app.update(
                  "DELETE FROM units WHERE id = ? OR unit_no IN (?, ?)",
                  probeId,
                  a.block().toString(),
                  b.block().toString()));
    }
  }

  @Test
  void i4MismatchedJwtAndSlugAreRejectedBeforeTheUnitProbe() throws Exception {
    mvc.perform(as(a, get(PROBE), b.slug())).andExpect(status().isUnauthorized());
  }

  @Test
  void i5RawAppQueryWithoutTenantContextSeesNoUnits() {
    assertThat(TenantContext.current()).isEmpty();
    assertThat(app.queryForObject("SELECT count(*) FROM units", Long.class)).isZero();
  }

  @Test
  void i6RawAppInsertWithoutTenantContextIsDenied() {
    assertThat(TenantContext.current()).isEmpty();
    var id = UUID.randomUUID();
    try {
      assertThatThrownBy(
              () ->
                  app.update(
                      "INSERT INTO units (id, tenant_id, block_id, unit_no) VALUES (?, ?, ?, ?)",
                      id,
                      a.id(),
                      a.block(),
                      id.toString()))
          .isInstanceOfSatisfying(
              DataAccessException.class,
              failure ->
                  assertThat(((SQLException) failure.getMostSpecificCause()).getSQLState())
                      .isEqualTo("42501"));
    } finally {
      scoped(a.id(), () -> app.update("DELETE FROM units WHERE id = ?", id));
    }
  }

  @Test
  void i7RuntimeRoleCannotSeeAnotherTenantsRowsEvenWithRawSql() {
    scoped(
        a.id(),
        () ->
            new TransactionTemplate(new JdbcTransactionManager(appSource))
                .executeWithoutResult(
                    status -> {
                      assertThat(app.queryForObject("SELECT current_user", String.class))
                          .isEqualTo("baiti_app");
                      assertThat(
                              app.queryForObject(
                                  "SELECT rolbypassrls FROM pg_roles WHERE rolname = current_user",
                                  Boolean.class))
                          .isFalse();
                      assertThat(
                              app.queryForList(
                                  "SELECT id FROM units WHERE id IN (?, ?)",
                                  UUID.class,
                                  a.unit(),
                                  b.unit()))
                          .containsExactly(a.unit());
                    }));
  }

  @Test
  void i8AsyncAuditEventsStayWithTheOriginatingTenant() throws Exception {
    var addressA = address();
    var addressB = address();
    var failedAddress = address();
    try (var first = new AuthenticationFixture(appSource, a.id(), AUDIT_TEST_PASSWORD);
        var second = new AuthenticationFixture(appSource, b.id(), AUDIT_TEST_PASSWORD)) {
      mvc.perform(signIn(a, first, addressA, AUDIT_TEST_PASSWORD)).andExpect(status().isOk());
      mvc.perform(signIn(b, second, addressB, AUDIT_TEST_PASSWORD)).andExpect(status().isOk());
      mvc.perform(signIn(a, first, failedAddress, "incorrect"))
          .andExpect(status().isUnauthorized());
      assertThat(workerContextCleared()).isTrue();
      assertThat(auditActors(a, addressA, "LOGIN_SUCCESS")).containsExactly(first.userId());
      assertThat(auditActors(b, addressB, "LOGIN_SUCCESS")).containsExactly(second.userId());
      assertThat(auditActors(a, addressB, "LOGIN_SUCCESS")).isEmpty();
      assertThat(auditActors(b, addressA, "LOGIN_SUCCESS")).isEmpty();
      assertThat(auditActors(a, failedAddress, "LOGIN_FAILED")).containsExactly((UUID) null);
      assertThat(auditActors(b, failedAddress, "LOGIN_FAILED")).isEmpty();
    } finally {
      workerContextCleared();
      removeAudit(a, addressA);
      removeAudit(b, addressB);
      removeAudit(a, failedAddress);
    }
  }

  private boolean workerContextCleared() throws Exception {
    return auditExecutor.submit(() -> TenantContext.current().isEmpty()).get(10, TimeUnit.SECONDS);
  }

  private MockHttpServletRequestBuilder signIn(
      Community community, AuthenticationFixture user, String address, String password) {
    return post("/api/auth/login")
        .with(
            request -> {
              request.setRemoteAddr(address);
              return request;
            })
        .header("X-Tenant-Slug", community.slug())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"" + user.email() + "\",\"password\":\"" + password + "\"}");
  }

  private List<UUID> auditActors(Community community, String address, String action) {
    return scopedResult(
        community.id(),
        jdbc ->
            jdbc.queryForList(
                "SELECT user_id FROM activity_logs WHERE action = ? AND ip_address = CAST(? AS inet)",
                UUID.class,
                action,
                address));
  }

  private void removeAudit(Community community, String address) {
    new TransactionTemplate(new JdbcTransactionManager(platform.getDataSource()))
        .executeWithoutResult(
            status -> {
              platform.queryForObject(
                  "SELECT set_config('app.current_tenant', ?, true)",
                  String.class,
                  community.id().toString());
              platform.update(
                  "DELETE FROM activity_logs WHERE tenant_id = ? AND ip_address = CAST(? AS inet)",
                  community.id(),
                  address);
            });
  }

  private static String address() {
    var bits = UUID.randomUUID().toString().replace("-", "");
    var address = new StringBuilder("2001:db8");
    for (int index = 0; index < 24; index += 4) {
      address.append(':').append(bits, index, index + 4);
    }
    return address.toString();
  }

  private Community fixture(String marker, String label) {
    var actor =
        UUID.nameUUIDFromBytes((marker + ":isolation-actor").getBytes(StandardCharsets.UTF_8));
    var key =
        UUID.nameUUIDFromBytes((marker + ":isolation-" + label).getBytes(StandardCharsets.UTF_8));
    var slug = "isolation-" + key;
    var name = "Synthetic isolation community " + label;
    var tenant =
        platform
            .queryForList("SELECT id FROM tenants WHERE slug = CAST(? AS citext)", UUID.class, slug)
            .stream()
            .findFirst()
            .orElseGet(
                () ->
                    onboarding
                        .create(
                            new PlatformPrincipal(actor),
                            new TenantRegistration(
                                slug,
                                name,
                                ADMIN_EMAIL,
                                "Synthetic admin",
                                UUID.randomUUID().toString()),
                            "127.0.0.1")
                        .tenantId());
    assertThat(
            platform.queryForObject(
                "SELECT name FROM tenants WHERE id = ? AND status = 'ACTIVE'",
                String.class,
                tenant))
        .isEqualTo(name);
    var ids =
        scopedResult(
            tenant,
            jdbc ->
                jdbc.queryForList(
                    "SELECT u.id FROM users u JOIN activity_logs a"
                        + " ON a.detail->>'adminUserId' = u.id::text"
                        + " WHERE a.action = 'TENANT_CREATED' AND a.detail->>'platformAdminId' = ?"
                        + " AND u.email = CAST(? AS citext)",
                    UUID.class,
                    actor.toString(),
                    ADMIN_EMAIL));
    assertThat(ids).hasSize(1);
    var block = UUID.randomUUID();
    var unit = UUID.randomUUID();
    scoped(
        tenant,
        () ->
            new TransactionTemplate(new JdbcTransactionManager(appSource))
                .executeWithoutResult(
                    status -> {
                      app.update(
                          "INSERT INTO blocks (id, tenant_id, name) VALUES (?, ?, ?)",
                          block,
                          tenant,
                          "Isolation block " + block);
                      app.update(
                          "INSERT INTO units (id, tenant_id, block_id, unit_no) VALUES (?, ?, ?, ?)",
                          unit,
                          tenant,
                          block,
                          "Isolation " + label);
                    }));
    return new Community(tenant, slug, ids.get(0), block, unit);
  }

  private void removeProperties(Community community) {
    if (community == null) {
      return;
    }
    scoped(
        community.id(),
        () -> {
          app.update("DELETE FROM units WHERE id = ?", community.unit());
          app.update("DELETE FROM blocks WHERE id = ?", community.block());
        });
  }

  private <T> T scopedResult(UUID tenant, Function<JdbcTemplate, T> action) {
    TenantContext.set(tenant);
    try {
      return action.apply(app);
    } finally {
      TenantContext.clear();
    }
  }

  private void scoped(UUID tenant, Runnable action) {
    TenantContext.set(tenant);
    try {
      action.run();
    } finally {
      TenantContext.clear();
    }
  }

  private MockHttpServletRequestBuilder as(
      Community community, MockHttpServletRequestBuilder request) {
    return as(community, request, community.slug());
  }

  private MockHttpServletRequestBuilder as(
      Community community, MockHttpServletRequestBuilder request, String resolvedSlug) {
    var principal =
        new TenantPrincipal(
            community.admin(),
            community.id(),
            Set.of("Admin"),
            Set.of("UNIT_READ_ALL", "UNIT_MANAGE"));
    return request
        .header("X-Tenant-Slug", resolvedSlug)
        .header("Authorization", "Bearer " + jwt.issue(principal));
  }

  private record Community(UUID id, String slug, UUID admin, UUID block, UUID unit) {}

  @RestController
  @RequestMapping(PROBE)
  static class UnitProbe {
    private final UnitRepository units;
    private final BlockRepository blocks;

    UnitProbe(UnitRepository units, BlockRepository blocks) {
      this.units = units;
      this.blocks = blocks;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('UNIT_READ_ALL')")
    List<UUID> list() {
      return units.findAll().stream().map(Unit::getId).toList();
    }

    @GetMapping("/{unit}")
    @PreAuthorize("hasAuthority('UNIT_READ_ALL')")
    UUID getUnit(@PathVariable UUID unit) {
      return units.findById(unit).orElseThrow(ResourceNotFoundException::new).getId();
    }

    @PostMapping("/{block}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('UNIT_MANAGE')")
    @Transactional
    void addUnit(@PathVariable UUID block) {
      var unit = new Unit();
      unit.setBlock(blocks.findById(block).orElseThrow(ResourceNotFoundException::new));
      unit.setUnitNo(block.toString());
      units.saveAndFlush(unit);
    }
  }
}
