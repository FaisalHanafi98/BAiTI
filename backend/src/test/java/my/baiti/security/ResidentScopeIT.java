package my.baiti.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.swagger.v3.oas.annotations.Hidden;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import my.baiti.testing.NativePostgresTestSupport;
import my.baiti.testing.ResidentScopeFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ResidentScopeIT.ScopeProbe.class)
class ResidentScopeIT extends NativePostgresTestSupport {

  private static final UUID A = UUID.fromString("ad8cdb3d-885a-4f0d-b678-2331ce582805");
  private static final UUID B = UUID.fromString("04b270a3-35dc-4bbd-b709-0942f6f7ef93");
  @Autowired private MockMvc mvc;
  @Autowired private JwtService jwt;

  @Autowired
  @Qualifier("appDataSource")
  private DataSource source;

  private ResidentScopeFixture first;
  private ResidentScopeFixture other;

  @BeforeEach
  void createScopedResidents() {
    first = ResidentScopeFixture.create(source, A);
    other = ResidentScopeFixture.create(source, B);
  }

  @AfterEach
  void removeOnlyOwnedRows() {
    try {
      if (other != null) {
        other.close();
      }
    } finally {
      if (first != null) {
        first.close();
      }
    }
  }

  @Test
  void anotherResidentInTheSameTenantIsHiddenWithAProblemResponse() throws Exception {
    var token = token(first.firstUser(), A);
    mvc.perform(
            get("/api/test/scope/residents/" + first.firstResident())
                .header("X-Tenant-Slug", "baiti-foundation-a")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    mvc.perform(
            get("/api/test/scope/residents/" + first.secondResident())
                .header("X-Tenant-Slug", "baiti-foundation-a")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("Resource not found"));
  }

  private String token(UUID user, UUID tenant) {
    return jwt.issue(
        new TenantPrincipal(
            user,
            tenant,
            Set.of("Resident"),
            Set.of("COMPLAINT_READ_OWN", "COMPLAINT_CREATE_OWN")));
  }

  @Test
  void foreignResidentAndUnknownResidentHaveTheSameNeutralDetail() throws Exception {
    var foreignPath = "/api/test/scope/residents/" + other.firstResident();
    mvc.perform(
            get(foreignPath)
                .header("X-Tenant-Slug", "baiti-foundation-b")
                .header("Authorization", "Bearer " + token(other.firstUser(), B)))
        .andExpect(status().isOk());
    for (var id : Set.of(other.firstResident(), UUID.randomUUID())) {
      mvc.perform(
              get("/api/test/scope/residents/" + id)
                  .header("X-Tenant-Slug", "baiti-foundation-a")
                  .header("Authorization", "Bearer " + token(first.firstUser(), A)))
          .andExpect(status().isNotFound())
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
          .andExpect(jsonPath("$.detail").value("Resource not found"));
    }
  }

  @Test
  void unitAccessRequiresAnActiveAssociationWhileOwnedHistoryRemainsLinked() throws Exception {
    var bearer = "Bearer " + token(first.firstUser(), A);
    var unitPath = "/api/test/scope/units/" + first.firstUnit();
    mvc.perform(
            get(unitPath)
                .header("X-Tenant-Slug", "baiti-foundation-a")
                .header("Authorization", bearer))
        .andExpect(status().isOk());
    for (var id : Set.of(first.secondUnit(), other.firstUnit())) {
      mvc.perform(
              get("/api/test/scope/units/" + id)
                  .header("X-Tenant-Slug", "baiti-foundation-a")
                  .header("Authorization", bearer))
          .andExpect(status().isNotFound());
    }
    first.moveFirstOut();
    mvc.perform(
            get(unitPath)
                .header("X-Tenant-Slug", "baiti-foundation-a")
                .header("Authorization", bearer))
        .andExpect(status().isNotFound());
    var residentPath = "/api/test/scope/residents/" + first.firstResident();
    mvc.perform(
            get(residentPath)
                .header("X-Tenant-Slug", "baiti-foundation-a")
                .header("Authorization", bearer))
        .andExpect(status().isOk());
    first.unlinkFirstUser();
    mvc.perform(
            get(residentPath)
                .header("X-Tenant-Slug", "baiti-foundation-a")
                .header("Authorization", bearer))
        .andExpect(status().isNotFound());
  }

  @Test
  void anOwnerStillNeedsTheNamedPermission() throws Exception {
    var token = jwt.issue(new TenantPrincipal(first.firstUser(), A, Set.of("Resident"), Set.of()));
    mvc.perform(
            get("/api/test/scope/residents/" + first.firstResident())
                .header("X-Tenant-Slug", "baiti-foundation-a")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Hidden
  @TestComponent
  @RestController
  public static class ScopeProbe {
    private final ResidentScopeGuard scope;

    public ScopeProbe(ResidentScopeGuard scope) {
      this.scope = scope;
    }

    @GetMapping("/api/test/scope/residents/{id}")
    @PreAuthorize("hasAuthority('COMPLAINT_READ_OWN')")
    public UUID resident(@PathVariable UUID id) {
      scope.requireResident(id);
      return id;
    }

    @GetMapping("/api/test/scope/units/{id}")
    @PreAuthorize("hasAuthority('COMPLAINT_CREATE_OWN')")
    public UUID unit(@PathVariable UUID id) {
      scope.requireActiveUnit(id);
      return id;
    }
  }
}
