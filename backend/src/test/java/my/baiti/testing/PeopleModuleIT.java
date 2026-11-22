package my.baiti.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import my.baiti.security.JwtService;
import my.baiti.security.TenantPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PeopleModuleIT extends NativePostgresTestSupport {
  private static final UUID A = UUID.fromString("ad8cdb3d-885a-4f0d-b678-2331ce582805");
  private static final UUID B = UUID.fromString("04b270a3-35dc-4bbd-b709-0942f6f7ef93");
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private final UUID actor = UUID.randomUUID();
  private final UUID role = UUID.randomUUID();
  private final String name = "P09-" + actor;
  private final String email = name + "@example.invalid";
  private final String remoteAddress =
      "127.0." + ((actor.hashCode() >>> 8) & 255) + "." + (actor.hashCode() & 255);
  @Autowired private MockMvc mvc;
  @Autowired private JwtService jwt;

  @BeforeEach
  void fixture() throws SQLException {
    try (var connection = connection(A)) {
      execute(
          connection, "INSERT INTO roles (id, tenant_id, name) VALUES (?, ?, ?)", role, A, name);
    }
  }

  @AfterEach
  void cleanup() throws SQLException {
    for (var tenant : Set.of(A, B)) {
      try (var connection = connection(tenant)) {
        execute(
            connection,
            "DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM users WHERE name = ?)",
            name);
        execute(connection, "DELETE FROM users WHERE name = ?", name);
        execute(connection, "DELETE FROM roles WHERE name LIKE ?", name + "%");
        execute(connection, "DELETE FROM activity_logs WHERE user_id = ?", actor);
      }
    }
  }

  @Test
  void createsAccountWithOneTimePasswordAndForcesPasswordReplacement() throws Exception {
    var response =
        call(post("/api/users"), A, createBody(role))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.name").value(name))
            .andExpect(jsonPath("$.user.mustChangePassword").value(true))
            .andReturn()
            .getResponse();
    assertThat(response.getHeader("Cache-Control")).contains("no-store");
    var created = JSON.readTree(response.getContentAsString());
    var password = created.get("temporaryPassword").asText();
    assertThat(password).hasSizeGreaterThanOrEqualTo(24);
    var login =
        mvc.perform(
                post("/api/auth/login")
                    .with(
                        request -> {
                          request.setRemoteAddr(remoteAddress);
                          return request;
                        })
                    .header("X-Tenant-Slug", "baiti-foundation-a")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JSON.writeValueAsString(Map.of("email", email, "password", password))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.mustChangePassword").value(true))
            .andReturn()
            .getResponse();
    assertThat(login.getHeader("Set-Cookie")).contains("Max-Age=0");
    var token = JSON.readTree(login.getContentAsString()).get("accessToken").asText();
    mvc.perform(
            get("/api/users")
                .header("X-Tenant-Slug", "baiti-foundation-a")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
    call(get("/api/users/{id}", created.get("user").get("id").asText()), A, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.temporaryPassword").doesNotExist())
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  private String createBody(UUID selectedRole) {
    return JSON.writeValueAsString(Map.of("name", name, "email", email, "roleId", selectedRole));
  }

  @Test
  void existingOpenApiOperationIdsRemainStable() throws Exception {
    var document =
        JSON.readTree(
            call(get("/v3/api-docs"), A, null)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    var paths = document.get("paths");
    assertThat(paths.get("/api/units").get("get").get("operationId").asText()).isEqualTo("list");
    assertThat(paths.get("/api/units").get("post").get("operationId").asText()).isEqualTo("create");
    assertThat(paths.get("/api/blocks").get("get").get("operationId").asText()).isEqualTo("list_1");
    assertThat(paths.get("/api/blocks").get("post").get("operationId").asText())
        .isEqualTo("create_1");
    assertThat(paths.get("/api/admin/tenants").get("get").get("operationId").asText())
        .isEqualTo("list_2");
    assertThat(paths.get("/api/admin/tenants").get("post").get("operationId").asText())
        .isEqualTo("create_2");
  }

  @Test
  void listsUsersAndRoleChoicesWithBoundedPaginationAndSafeSorts() throws Exception {
    call(post("/api/users"), A, createBody(role)).andExpect(status().isCreated());
    call(get("/api/users").param("size", "101"), A, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(100))
        .andExpect(jsonPath("$.content[*].email").value(org.hamcrest.Matchers.hasItem(email)))
        .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
    call(get("/api/roles").param("size", "1").param("sort", "name,desc"), A, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(1));
    for (var path : Set.of("/api/users", "/api/roles")) {
      call(get(path).param("sort", "tenantId"), A, null).andExpect(status().isBadRequest());
      call(get(path).param("sort", "passwordHash"), A, null).andExpect(status().isBadRequest());
    }
  }

  @Test
  void assigningRoleRevokesRefreshCredentialsAndDeactivationBlocksLogin() throws Exception {
    var account =
        JSON.readTree(
            call(post("/api/users"), A, createBody(role))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    var id = account.get("user").get("id").asText();
    var temporary = account.get("temporaryPassword").asText();
    var first = login(temporary).andExpect(status().isOk());
    var restricted =
        JSON.readTree(first.andReturn().getResponse().getContentAsString())
            .get("accessToken")
            .asText();
    var replacement = "Synthetic-replacement-" + actor;
    mvc.perform(
            post("/api/auth/password")
                .with(
                    request -> {
                      request.setRemoteAddr(remoteAddress);
                      return request;
                    })
                .header("X-Tenant-Slug", "baiti-foundation-a")
                .header("Authorization", "Bearer " + restricted)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    JSON.writeValueAsString(
                        Map.of("currentPassword", temporary, "newPassword", replacement))))
        .andExpect(status().isNoContent());
    var signedIn = login(replacement).andExpect(status().isOk()).andReturn().getResponse();
    var refresh = signedIn.getCookie("baiti_refresh").getValue();
    var otherRole = UUID.randomUUID();
    try (var connection = connection(A)) {
      execute(
          connection,
          "INSERT INTO roles (id, tenant_id, name) VALUES (?, ?, ?)",
          otherRole,
          A,
          name + "-other");
    }
    call(put("/api/users/{id}/role", id), A, JSON.writeValueAsString(Map.of("roleId", otherRole)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roleId").value(otherRole.toString()));
    mvc.perform(
            post("/api/auth/refresh")
                .header("X-Tenant-Slug", "baiti-foundation-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("token", refresh))))
        .andExpect(status().isUnauthorized());
    var current =
        login(replacement)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.role").value(name + "-other"))
            .andReturn()
            .getResponse()
            .getCookie("baiti_refresh")
            .getValue();
    call(post("/api/users/{id}/deactivate", id), A, null).andExpect(status().isNoContent());
    call(post("/api/users/{id}/deactivate", id), A, null).andExpect(status().isNoContent());
    call(get("/api/users/{id}", id), A, null).andExpect(jsonPath("$.status").value("INACTIVE"));
    login(replacement).andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/auth/refresh")
                .header("X-Tenant-Slug", "baiti-foundation-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("token", current))))
        .andExpect(status().isUnauthorized());
    try (var connection = connection(A);
        var statement =
            connection.prepareStatement(
                "SELECT action, count(*) FROM activity_logs WHERE user_id = ? GROUP BY action")) {
      statement.setObject(1, actor);
      try (var rows = statement.executeQuery()) {
        var actions = new java.util.HashMap<String, Long>();
        while (rows.next()) actions.put(rows.getString(1), rows.getLong(2));
        assertThat(actions)
            .containsEntry("USER_CREATED", 1L)
            .containsEntry("USER_ROLE_CHANGED", 1L)
            .containsEntry("USER_DEACTIVATED", 1L);
      }
    }
  }

  private ResultActions login(String password) throws Exception {
    return mvc.perform(
        post("/api/auth/login")
            .with(
                request -> {
                  request.setRemoteAddr(remoteAddress);
                  return request;
                })
            .header("X-Tenant-Slug", "baiti-foundation-a")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JSON.writeValueAsString(Map.of("email", email, "password", password))));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Manager", "Resident", "Guard", "Admin"})
  void roleLabelsWithoutNamedPermissionsCannotManageAccounts(String roleName) throws Exception {
    var token = jwt.issue(new TenantPrincipal(actor, A, Set.of(roleName), Set.of()));
    for (var request :
        java.util.List.of(
            get("/api/users"),
            get("/api/users/{id}", actor),
            post("/api/users").contentType(MediaType.APPLICATION_JSON).content(createBody(role)),
            put("/api/users/{id}/role", actor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("roleId", role))),
            post("/api/users/{id}/deactivate", actor),
            get("/api/roles"))) {
      mvc.perform(
              request
                  .header("X-Tenant-Slug", "baiti-foundation-a")
                  .header("Authorization", "Bearer " + token))
          .andExpect(status().isForbidden());
    }
  }

  @Test
  void userPermissionAllowsRoleChoicesAndRolePermissionAloneCannotManageUsers() throws Exception {
    var userToken =
        jwt.issue(new TenantPrincipal(actor, A, Set.of("Custom"), Set.of("USER_MANAGE")));
    var roleToken =
        jwt.issue(new TenantPrincipal(actor, A, Set.of("Custom"), Set.of("ROLE_MANAGE")));
    for (var token : Set.of(userToken, roleToken)) {
      mvc.perform(
              get("/api/roles")
                  .header("X-Tenant-Slug", "baiti-foundation-a")
                  .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk());
    }
    mvc.perform(
            get("/api/users")
                .header("X-Tenant-Slug", "baiti-foundation-a")
                .header("Authorization", "Bearer " + roleToken))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/users").header("X-Tenant-Slug", "baiti-foundation-a"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void foreignUsersAndRolesAreInvisibleAndCannotBeAssigned() throws Exception {
    var foreignRole = UUID.randomUUID();
    try (var connection = connection(B)) {
      execute(
          connection,
          "INSERT INTO roles (id, tenant_id, name) VALUES (?, ?, ?)",
          foreignRole,
          B,
          name);
    }
    var foreign =
        JSON.readTree(
                call(post("/api/users"), B, createBody(foreignRole))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("user")
            .get("id")
            .asText();
    var own =
        JSON.readTree(
                call(post("/api/users"), A, createBody(role))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("user")
            .get("id")
            .asText();
    call(get("/api/users/{id}", foreign), B, null).andExpect(status().isOk());
    call(get("/api/users"), A, null)
        .andExpect(
            jsonPath("$.content[*].id")
                .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(foreign))));
    call(get("/api/roles"), A, null)
        .andExpect(
            jsonPath("$.content[*].id")
                .value(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem(foreignRole.toString()))));
    for (var id : Set.of(foreign, UUID.randomUUID().toString())) {
      call(get("/api/users/{id}", id), A, null)
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.detail").value("Resource not found"));
      call(post("/api/users/{id}/deactivate", id), A, null).andExpect(status().isNotFound());
      call(put("/api/users/{id}/role", id), A, JSON.writeValueAsString(Map.of("roleId", role)))
          .andExpect(status().isNotFound());
    }
    for (var id : Set.of(foreignRole, UUID.randomUUID())) {
      call(post("/api/users"), A, createBody(id)).andExpect(status().isNotFound());
      call(put("/api/users/{id}/role", own), A, JSON.writeValueAsString(Map.of("roleId", id)))
          .andExpect(status().isNotFound());
    }
    call(get("/api/users/{id}", own), A, null)
        .andExpect(jsonPath("$.roleId").value(role.toString()));
  }

  @Test
  void rejectsInvalidAccountsAndCaseInsensitiveDuplicateEmailsWithoutExtraAudit() throws Exception {
    for (var body :
        java.util.List.of(
            "{}", "{\"name\":\" \",\"email\":\"invalid\"}", "{\"roleId\":\"invalid\"}", "{")) {
      call(post("/api/users"), A, body).andExpect(status().isBadRequest());
    }
    var created =
        JSON.readTree(
            call(post("/api/users"), A, createBody(role))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    call(
            post("/api/users"),
            A,
            JSON.writeValueAsString(
                Map.of(
                    "name",
                    name,
                    "email",
                    email.toUpperCase(java.util.Locale.ROOT),
                    "roleId",
                    role)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errors[0].field").value("email"));
    var id = created.get("user").get("id").asText();
    call(put("/api/users/{id}/role", id), A, "{}").andExpect(status().isBadRequest());
    call(put("/api/users/{id}/role", id), A, JSON.writeValueAsString(Map.of("roleId", role)))
        .andExpect(status().isOk());
    call(get("/api/users/not-a-uuid"), A, null).andExpect(status().isBadRequest());
    try (var connection = connection(A);
        var statement =
            connection.prepareStatement("SELECT count(*) FROM activity_logs WHERE user_id = ?")) {
      statement.setObject(1, actor);
      try (var rows = statement.executeQuery()) {
        assertThat(rows.next()).isTrue();
        assertThat(rows.getInt(1)).isEqualTo(1);
      }
    }
    assertThat(new my.baiti.people.CreatedUserResponse(null, "synthetic-secret").toString())
        .doesNotContain("synthetic-secret");
  }

  private ResultActions call(MockHttpServletRequestBuilder request, UUID tenant, String body)
      throws Exception {
    var token =
        jwt.issue(
            new TenantPrincipal(
                actor, tenant, Set.of("Admin"), Set.of("USER_MANAGE", "ROLE_MANAGE")));
    request
        .header("X-Tenant-Slug", tenant.equals(A) ? "baiti-foundation-a" : "baiti-foundation-b")
        .header("Authorization", "Bearer " + token);
    if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
    return mvc.perform(request);
  }

  private static Connection connection(UUID tenant) throws SQLException {
    var connection = NativePostgresConfig.from(System.getenv()).openMigrationConnection();
    try {
      execute(connection, "SELECT set_config('app.current_tenant', ?, false)", tenant.toString());
      return connection;
    } catch (SQLException failure) {
      connection.close();
      throw failure;
    }
  }

  private static void execute(Connection connection, String sql, Object... values)
      throws SQLException {
    try (var statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
      statement.execute();
    }
  }
}
