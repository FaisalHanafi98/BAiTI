package my.baiti.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.swagger.v3.oas.annotations.Hidden;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import my.baiti.people.DefaultRoleGrants;
import my.baiti.testing.NativePostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PermissionMatrixIT.PermissionProbe.class)
class PermissionMatrixIT extends NativePostgresTestSupport {

  private static final UUID A = UUID.fromString("ad8cdb3d-885a-4f0d-b678-2331ce582805");
  @Autowired private MockMvc mvc;
  @Autowired private JwtService jwt;

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  private RequestMappingHandlerMapping mappings;

  @ParameterizedTest
  @ValueSource(strings = {"Admin", "Manager", "Resident", "Guard"})
  void everyDefaultRoleHasTheExpectedAllowAndDenyResults(String role) throws Exception {
    var token =
        jwt.issue(
            new TenantPrincipal(
                UUID.randomUUID(),
                A,
                Set.of(role),
                DefaultRoleGrants.forRole(role).stream()
                    .map(Enum::name)
                    .collect(Collectors.toSet())));
    try (var reader = fixture("permission-matrix.csv")) {
      int roleColumn = Arrays.asList(reader.readLine().split(",")).indexOf(role);
      assertThat(roleColumn).isGreaterThanOrEqualTo(2);
      for (var row : reader.lines().map(line -> line.split(",")).toList()) {
        mvc.perform(
                get("/api/test/security/permissions/" + row[0])
                    .header("X-Tenant-Slug", "baiti-foundation-a")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().is(Boolean.parseBoolean(row[roleColumn]) ? 200 : 403));
      }
    }
  }

  @Test
  void everyApplicationEndpointHasAnExplicitClassificationAndAppearsInOpenApi() throws Exception {
    Set<String> classified;
    try (var reader = fixture("endpoint-access.csv")) {
      classified =
          reader
              .lines()
              .skip(1)
              .map(line -> line.split(","))
              .map(row -> row[0] + " " + row[1])
              .collect(Collectors.toSet());
    }
    var registered = new TreeSet<String>();
    mappings
        .getHandlerMethods()
        .forEach(
            (mapping, handler) -> {
              if (!handler.getBeanType().getPackageName().startsWith("my.baiti")
                  || PermissionProbe.class.isAssignableFrom(handler.getBeanType())) {
                return;
              }
              assertThat(mapping.getMethodsCondition().getMethods()).isNotEmpty();
              mapping
                  .getPatternValues()
                  .forEach(
                      path ->
                          mapping
                              .getMethodsCondition()
                              .getMethods()
                              .forEach(method -> registered.add(method.name() + " " + path)));
            });
    assertThat(registered).containsExactlyInAnyOrderElementsOf(classified);
    var token = jwt.issue(new TenantPrincipal(UUID.randomUUID(), A, Set.of("Admin"), Set.of()));
    var body =
        mvc.perform(
                get("/v3/api-docs")
                    .header("X-Tenant-Slug", "baiti-foundation-a")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var paths = JsonMapper.builder().build().readTree(body).get("paths");
    var documented = new TreeSet<String>();
    paths
        .properties()
        .forEach(
            entry -> {
              for (var method : RequestMethod.values()) {
                if (entry.getValue().has(method.name().toLowerCase(Locale.ROOT))) {
                  documented.add(method.name() + " " + entry.getKey());
                }
              }
            });
    assertThat(documented).containsExactlyInAnyOrderElementsOf(classified);
  }

  private BufferedReader fixture(String name) {
    return new BufferedReader(
        new InputStreamReader(
            getClass().getResourceAsStream("/fixtures/" + name), StandardCharsets.UTF_8));
  }

  @Test
  void anAdminRoleWithoutTheNamedPermissionIsDenied() throws Exception {
    var token = jwt.issue(new TenantPrincipal(UUID.randomUUID(), A, Set.of("Admin"), Set.of()));
    mvc.perform(
            get("/api/test/security/permissions/BILL_MANAGE")
                .header("X-Tenant-Slug", "baiti-foundation-a")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Hidden
  @RestController
  public static class PermissionProbe {

    @GetMapping("/api/test/security/permissions/{permission}")
    @PreAuthorize("hasAuthority(#permission)")
    public String permitted(@PathVariable String permission) {
      return permission;
    }
  }
}
