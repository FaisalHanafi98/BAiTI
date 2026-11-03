package my.baiti.testing;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import my.baiti.config.DataSourceConfig;
import my.baiti.config.JpaConfig;
import my.baiti.people.Permission;
import my.baiti.people.PermissionRepository;
import my.baiti.people.Role;
import my.baiti.people.RoleRepository;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({DataSourceConfig.class, JpaConfig.class, FoundationTenantConfiguration.class})
class CoreRepositoryIT extends NativePostgresTestSupport {

  private final EntityManager entityManager;
  private final RoleRepository roles;
  private final PermissionRepository permissions;

  @Autowired
  CoreRepositoryIT(
      EntityManager entityManager, RoleRepository roles, PermissionRepository permissions) {
    this.entityManager = entityManager;
    this.roles = roles;
    this.permissions = permissions;
  }

  @BeforeEach
  void bindTheFixtureTransactionToTenantA() {
    entityManager
        .unwrap(Session.class)
        .doWork(
            connection -> {
              try (var statement =
                  connection.prepareStatement("SELECT set_config('app.current_tenant', ?, true)")) {
                statement.setString(1, FoundationTenantConfiguration.TENANT.toString());
                statement.execute();
              }
            });
    assertThat(
            entityManager
                .createNativeQuery("SELECT name FROM tenants WHERE id = :id", String.class)
                .setParameter("id", FoundationTenantConfiguration.TENANT)
                .getSingleResult())
        .isEqualTo("Synthetic foundation tenant A");
  }

  @Test
  void roleAndPermissionGrantRoundTripWithTheirTenant() {
    var permission = new Permission();
    permission.setCode("F0_" + UUID.randomUUID());
    permission.setModule("FOUNDATION");
    permissions.saveAndFlush(permission);
    var role = new Role();
    role.setName("F0_" + UUID.randomUUID());
    role.grantPermission(permission);
    roles.saveAndFlush(role);
    UUID roleId = role.getId();
    entityManager.clear();

    var restored = roles.findById(roleId).orElseThrow();
    assertThat(restored.getTenantId()).isEqualTo(FoundationTenantConfiguration.TENANT);
    assertThat(restored.getPermissions())
        .extracting(Permission::getId)
        .containsExactly(permission.getId());
    assertThat(
            entityManager
                .createNativeQuery(
                    "SELECT tenant_id FROM roles_permissions WHERE role_id = :id", UUID.class)
                .setParameter("id", roleId)
                .getSingleResult())
        .isEqualTo(FoundationTenantConfiguration.TENANT);
  }

  @Test
  void revokingAGrantDeletesTheLinkAndPreservesThePermission() {
    var permission = new Permission();
    permission.setCode("F0_" + UUID.randomUUID());
    permission.setModule("FOUNDATION");
    permissions.saveAndFlush(permission);
    var role = new Role();
    role.setName("F0_" + UUID.randomUUID());
    role.grantPermission(permission);
    roles.saveAndFlush(role);
    UUID roleId = role.getId();
    UUID permissionId = permission.getId();
    entityManager.clear();
    var restored = roles.findById(roleId).orElseThrow();

    assertThat(restored.revokePermission(permissions.findById(permissionId).orElseThrow()))
        .isTrue();
    roles.flush();
    entityManager.clear();

    assertThat(roles.findById(roleId).orElseThrow().getPermissions()).isEmpty();
    assertThat(permissions.findById(permissionId)).isPresent();
    assertThat(
            entityManager
                .createNativeQuery(
                    "SELECT count(*) FROM roles_permissions WHERE role_id = :id", Long.class)
                .setParameter("id", roleId)
                .getSingleResult())
        .isEqualTo(0L);
  }
}
