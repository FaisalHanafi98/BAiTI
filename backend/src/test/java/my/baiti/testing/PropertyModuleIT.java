package my.baiti.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import my.baiti.people.DefaultRoleGrants;
import my.baiti.security.JwtService;
import my.baiti.security.TenantPrincipal;
import org.junit.jupiter.api.AfterEach;
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
class PropertyModuleIT extends NativePostgresTestSupport {
  private static final UUID A = UUID.fromString("ad8cdb3d-885a-4f0d-b678-2331ce582805");
  private static final UUID B = UUID.fromString("04b270a3-35dc-4bbd-b709-0942f6f7ef93");
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private final UUID actor = UUID.randomUUID();
  private final String prefix = "P08-" + actor;
  @Autowired private MockMvc mvc;
  @Autowired private JwtService jwt;

  @AfterEach
  void removeTestRows() throws SQLException {
    for (var tenant : Set.of(A, B)) {
      try (var connection = connection(tenant)) {
        execute(connection, "DELETE FROM residents WHERE name = ?", prefix);
        execute(
            connection,
            "DELETE FROM units WHERE block_id IN (SELECT id FROM blocks WHERE name LIKE ?)",
            prefix + "%");
        execute(connection, "DELETE FROM blocks WHERE name LIKE ?", prefix + "%");
        execute(connection, "DELETE FROM activity_logs WHERE user_id = ?", actor);
      }
    }
  }

  @Test
  void adminCanManageBlocksAndUnitsAndSuccessfulWritesAreAudited() throws Exception {
    var block =
        id(
            call(post("/api/blocks"), A, "Admin", "{\"name\":\"" + prefix + "\"}")
                .andExpect(status().isCreated()));
    call(get("/api/blocks/{id}", block), A, "Admin", null)
        .andExpect(jsonPath("$.name").value(prefix));
    call(
            put("/api/blocks/{id}", block),
            A,
            "Admin",
            "{\"name\":\"" + prefix + "-updated\",\"description\":\"Main entrance\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("Main entrance"));
    var unit =
        id(
            call(post("/api/units"), A, "Admin", unitBody(block, "2A", "VACANT"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("VACANT")));
    call(put("/api/units/{id}", unit), A, "Admin", unitBody(block, "2B", "OCCUPIED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("OCCUPIED"));
    call(get("/api/units/{id}", unit), A, "Manager", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unitNo").value("2B"))
        .andExpect(jsonPath("$.blockId").value(block.toString()));
    call(delete("/api/units/{id}", unit), A, "Admin", null).andExpect(status().isNoContent());
    call(delete("/api/blocks/{id}", block), A, "Admin", null).andExpect(status().isNoContent());
    call(get("/api/units/{id}", unit), A, "Admin", null).andExpect(status().isNotFound());
    call(get("/api/blocks/{id}", block), A, "Admin", null).andExpect(status().isNotFound());
    assertAudit(A, "BLOCK_CREATED", block);
    assertAudit(A, "BLOCK_UPDATED", block);
    assertAudit(A, "BLOCK_DELETED", block);
    assertAudit(A, "UNIT_CREATED", unit);
    assertAudit(A, "UNIT_UPDATED", unit);
    assertAudit(A, "UNIT_DELETED", unit);
  }

  @Test
  void unitsCanBeFilteredByBlockAndOccupancyAndSortedAcrossPages() throws Exception {
    var block = block(A, "-A");
    var otherBlock = block(A, "-Z");
    unit(A, block, "2", "OCCUPIED");
    unit(A, block, "1", "OCCUPIED");
    unit(A, block, "0", "VACANT");
    unit(A, otherBlock, "0", "OCCUPIED");
    call(
            get("/api/units")
                .param("blockId", block.toString())
                .param("status", "OCCUPIED")
                .param("size", "1"),
            A,
            "Manager",
            null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].unitNo").value("1"))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(2))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(1));
    call(
            get("/api/units")
                .param("blockId", block.toString())
                .param("status", "OCCUPIED")
                .param("size", "1")
                .param("page", "1"),
            A,
            "Manager",
            null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].unitNo").value("2"));
    call(get("/api/units").param("status", "OCCUPIED").param("size", "101"), A, "Manager", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].unitNo").value("1"))
        .andExpect(jsonPath("$.content[1].unitNo").value("2"))
        .andExpect(jsonPath("$.content[2].unitNo").value("0"))
        .andExpect(jsonPath("$.size").value(100));
    call(
            get("/api/units").param("blockId", block.toString()).param("sort", "unitNo,desc"),
            A,
            "Manager",
            null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].unitNo").value("2"));
    call(get("/api/blocks").param("size", "1"), A, "Admin", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").exists())
        .andExpect(jsonPath("$.size").value(1));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Manager", "Resident", "Guard"})
  void defaultNonAdminRolesCannotManageProperty(String role) throws Exception {
    var block = block(A, "-role");
    var unit = unit(A, block, "6A", "OCCUPIED");
    call(get("/api/blocks"), A, role, null).andExpect(status().isForbidden());
    call(get("/api/blocks/{id}", block), A, role, null).andExpect(status().isForbidden());
    call(post("/api/blocks"), A, role, "{\"name\":\"" + prefix + "-denied\"}")
        .andExpect(status().isForbidden());
    call(put("/api/blocks/{id}", block), A, role, "{\"name\":\"" + prefix + "-denied\"}")
        .andExpect(status().isForbidden());
    call(delete("/api/blocks/{id}", block), A, role, null).andExpect(status().isForbidden());
    call(post("/api/units"), A, role, unitBody(block, "7B", "VACANT"))
        .andExpect(status().isForbidden());
    call(put("/api/units/{id}", unit), A, role, unitBody(block, "7B", "VACANT"))
        .andExpect(status().isForbidden());
    call(delete("/api/units/{id}", unit), A, role, null).andExpect(status().isForbidden());
    call(get("/api/units"), A, role, null)
        .andExpect(status().is(role.equals("Resident") ? 403 : 200));
    call(get("/api/units/{id}", unit), A, role, null)
        .andExpect(status().is(role.equals("Resident") ? 403 : 200));
  }

  @Test
  void guardReadsOnlyUnitNumbersAndCannotQueryHiddenMetadata() throws Exception {
    var block = block(A, "-guard");
    var unit = unit(A, block, "GUARD", "OCCUPIED");
    call(get("/api/units/{id}", unit), A, "Guard", null)
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    "{\"unitNo\":\"GUARD\"}",
                    org.springframework.test.json.JsonCompareMode.STRICT));
    call(get("/api/units"), A, "Guard", null)
        .andExpect(jsonPath("$.content[0].unitNo").value("GUARD"))
        .andExpect(jsonPath("$.content[0].id").doesNotExist())
        .andExpect(jsonPath("$.content[0].blockId").doesNotExist())
        .andExpect(jsonPath("$.content[0].status").doesNotExist());
    call(get("/api/units").param("status", "OCCUPIED"), A, "Guard", null)
        .andExpect(status().isForbidden());
    call(get("/api/units").param("blockId", block.toString()), A, "Guard", null)
        .andExpect(status().isForbidden());
    call(get("/api/units").param("sort", "status"), A, "Guard", null)
        .andExpect(status().isBadRequest());
  }

  @Test
  void foreignAndMissingPropertyHaveTheSameNeutralResponse() throws Exception {
    var foreignBlock = block(B, "-foreign");
    var foreignUnit = unit(B, foreignBlock, "9A", "OCCUPIED");
    var ownBlock = block(A, "-own");
    var ownUnit = unit(A, ownBlock, "9A", "VACANT");
    call(get("/api/blocks/{id}", foreignBlock), B, "Admin", null).andExpect(status().isOk());
    call(get("/api/units/{id}", foreignUnit), B, "Manager", null).andExpect(status().isOk());
    call(get("/api/units"), A, "Manager", null)
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.content[*].id")
                .value(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem(foreignUnit.toString()))));
    for (var block : Set.of(foreignBlock, UUID.randomUUID())) {
      missing(call(get("/api/blocks/{id}", block), A, "Admin", null));
      missing(
          call(put("/api/blocks/{id}", block), A, "Admin", "{\"name\":\"" + prefix + "-other\"}"));
      missing(call(delete("/api/blocks/{id}", block), A, "Admin", null));
      missing(call(get("/api/units").param("blockId", block.toString()), A, "Manager", null));
      missing(call(post("/api/units"), A, "Admin", unitBody(block, "8A", "VACANT")));
      missing(call(put("/api/units/{id}", ownUnit), A, "Admin", unitBody(block, "8B", "OCCUPIED")));
    }
    for (var unit : Set.of(foreignUnit, UUID.randomUUID())) {
      missing(call(get("/api/units/{id}", unit), A, "Manager", null));
      missing(call(put("/api/units/{id}", unit), A, "Admin", unitBody(ownBlock, "8A", "VACANT")));
      missing(call(delete("/api/units/{id}", unit), A, "Admin", null));
    }
    call(get("/api/units/{id}", ownUnit), A, "Manager", null)
        .andExpect(jsonPath("$.unitNo").value("9A"));
  }

  @Test
  void uniquenessAndDependentRowsReturnConflictsWithoutChangingProperty() throws Exception {
    var block = block(A, "-main");
    var spare = block(A, "-spare");
    var unit = unit(A, block, "2A", "VACANT");
    var spareUnit = unit(A, block, "2B", "OCCUPIED");
    call(post("/api/blocks"), A, "Admin", "{\"name\":\"" + prefix + "-main\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errors[0].field").value("name"));
    call(put("/api/blocks/{id}", spare), A, "Admin", "{\"name\":\"" + prefix + "-main\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errors[0].field").value("name"));
    call(post("/api/units"), A, "Admin", unitBody(block, "2A", "VACANT"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errors[0].field").value("unitNo"));
    call(put("/api/units/{id}", spareUnit), A, "Admin", unitBody(block, "2A", "VACANT"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errors[0].field").value("unitNo"));
    call(delete("/api/blocks/{id}", block), A, "Admin", null)
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    try (var connection = connection(A)) {
      execute(
          connection,
          "INSERT INTO residents (tenant_id, unit_id, name, resident_type) VALUES (?, ?, ?, 'OWNER')",
          A,
          unit,
          prefix);
    }
    call(delete("/api/units/{id}", unit), A, "Admin", null)
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    call(get("/api/units/{id}", spareUnit), A, "Manager", null)
        .andExpect(jsonPath("$.status").value("OCCUPIED"));
    call(get("/api/units/{id}", unit), A, "Manager", null).andExpect(status().isOk());
    try (var connection = connection(A);
        var statement =
            connection.prepareStatement("SELECT count(*) FROM activity_logs WHERE user_id = ?")) {
      statement.setObject(1, actor);
      try (var rows = statement.executeQuery()) {
        assertThat(rows.next()).isTrue();
        assertThat(rows.getInt(1)).isZero();
      }
    }
  }

  @Test
  void invalidRequestsReturnProblemDetails() throws Exception {
    var block = block(A, "-validation");
    call(post("/api/blocks"), A, "Admin", "{\"name\":\" \"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("name"));
    call(post("/api/units"), A, "Admin", unitBody(block, " ", "VACANT"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("unitNo"));
    call(post("/api/units"), A, "Admin", "{\"unitNo\":\"1A\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.hasItem("blockId")));
    call(post("/api/units"), A, "Admin", unitBody(block, "1A", "UNKNOWN"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    call(get("/api/units").param("status", "UNKNOWN"), A, "Manager", null)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("status"));
    call(get("/api/units").param("sort", "tenantId"), A, "Manager", null)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("sort"));
  }

  private ResultActions call(
      MockHttpServletRequestBuilder request, UUID tenant, String role, String body)
      throws Exception {
    var permissions =
        DefaultRoleGrants.forRole(role).stream().map(Enum::name).collect(Collectors.toSet());
    var token = jwt.issue(new TenantPrincipal(actor, tenant, Set.of(role), permissions));
    request
        .header("X-Tenant-Slug", tenant.equals(A) ? "baiti-foundation-a" : "baiti-foundation-b")
        .header("Authorization", "Bearer " + token);
    if (body != null) {
      request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
    return mvc.perform(request);
  }

  private static UUID id(ResultActions result) throws Exception {
    return UUID.fromString(
        JSON.readTree(result.andReturn().getResponse().getContentAsString()).get("id").asText());
  }

  private static void missing(ResultActions result) throws Exception {
    result
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("Resource not found"));
  }

  private static String unitBody(UUID block, String unitNo, String status) {
    return JSON.writeValueAsString(
        java.util.Map.of("blockId", block, "unitNo", unitNo, "status", status));
  }

  private UUID block(UUID tenant, String suffix) throws SQLException {
    var id = UUID.randomUUID();
    try (var connection = connection(tenant)) {
      execute(
          connection,
          "INSERT INTO blocks (id, tenant_id, name) VALUES (?, ?, ?)",
          id,
          tenant,
          prefix + suffix);
    }
    return id;
  }

  private static UUID unit(UUID tenant, UUID block, String number, String status)
      throws SQLException {
    var id = UUID.randomUUID();
    try (var connection = connection(tenant)) {
      execute(
          connection,
          "INSERT INTO units (id, tenant_id, block_id, unit_no, status) VALUES (?, ?, ?, ?, ?)",
          id,
          tenant,
          block,
          number,
          status);
    }
    return id;
  }

  private void assertAudit(UUID tenant, String action, UUID resource) throws SQLException {
    try (var connection = connection(tenant);
        var statement =
            connection.prepareStatement(
                "SELECT detail->>'resourceId' FROM activity_logs WHERE user_id = ? AND action = ?")) {
      statement.setObject(1, actor);
      statement.setString(2, action);
      try (var rows = statement.executeQuery()) {
        assertThat(rows.next()).isTrue();
        assertThat(rows.getString(1)).isEqualTo(resource.toString());
        assertThat(rows.next()).isFalse();
      }
    }
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
      for (int i = 0; i < values.length; i++) {
        statement.setObject(i + 1, values[i]);
      }
      statement.execute();
    }
  }
}
