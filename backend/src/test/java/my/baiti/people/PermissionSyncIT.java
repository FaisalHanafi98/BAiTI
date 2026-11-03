package my.baiti.people;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import my.baiti.config.DataSourceConfig;
import my.baiti.config.JpaConfig;
import my.baiti.tenancy.TenantContext;
import my.baiti.testing.NativePostgresTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({DataSourceConfig.class, JpaConfig.class, PermissionSyncService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PermissionSyncIT extends NativePostgresTestSupport {

  private static final UUID A = UUID.fromString("ad8cdb3d-885a-4f0d-b678-2331ce582805");
  @Autowired private PermissionSyncService sync;
  @Autowired private PermissionRepository permissions;
  @Autowired private RoleRepository roles;
  @Autowired private EntityManager entityManager;
  @Autowired private PlatformTransactionManager transactions;

  @AfterEach
  void clearContext() {
    TenantContext.clear();
  }

  @Test
  void initialSyncCreatesTheTenantCatalogueAndExactDefaultGrantsThroughTheAppRole() {
    TenantContext.set(A);
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              status.setRollbackOnly();
              sync.synchronizeCurrentTenant();
              entityManager.clear();
              assertThat(
                      entityManager
                          .createNativeQuery("SELECT current_user", String.class)
                          .getSingleResult())
                  .isEqualTo("baiti_app");
              assertThat(permissions.findAll().stream().map(Permission::getCode))
                  .containsAll(Arrays.stream(PermissionCatalog.values()).map(Enum::name).toList());
              DefaultRoleGrants.all()
                  .forEach(
                      (name, grants) -> {
                        var role = roles.findByName(name).orElseThrow();
                        assertThat(role.getTenantId()).isEqualTo(A);
                        assertThat(role.isSystem()).isTrue();
                        assertThat(
                                role.getPermissions().stream()
                                    .map(Permission::getCode)
                                    .collect(Collectors.toSet()))
                            .containsExactlyInAnyOrderElementsOf(
                                grants.stream().map(Enum::name).toList());
                      });
            });
  }

  @Test
  void repeatingSyncKeepsExistingIdsAndDoesNotRestoreARevokedGrant() {
    TenantContext.set(A);
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              status.setRollbackOnly();
              sync.synchronizeCurrentTenant();
              var ids =
                  permissions.findAll().stream()
                      .collect(Collectors.toMap(Permission::getCode, Permission::getId));
              var admin = roles.findByName("Admin").orElseThrow();
              var adminId = admin.getId();
              admin.revokePermission(permissions.findByCode("BILL_MANAGE").orElseThrow());
              roles.saveAndFlush(admin);
              entityManager.clear();
              sync.synchronizeCurrentTenant();
              entityManager.clear();
              assertThat(
                      permissions.findAll().stream()
                          .collect(Collectors.toMap(Permission::getCode, Permission::getId)))
                  .containsExactlyInAnyOrderEntriesOf(ids);
              var repeated = roles.findByName("Admin").orElseThrow();
              assertThat(repeated.getId()).isEqualTo(adminId);
              assertThat(repeated.getPermissions().stream().map(Permission::getCode))
                  .doesNotContain("BILL_MANAGE")
                  .contains("USER_MANAGE");
            });
  }

  @Test
  void onlyNewCatalogueCodesReceiveDefaultsOnAnExistingSystemRole() {
    TenantContext.set(A);
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              status.setRollbackOnly();
              var existing = new Permission();
              existing.setCode("BILL_MANAGE");
              existing.setModule("finance");
              permissions.saveAndFlush(existing);
              var admin = new Role();
              admin.setName("Admin");
              admin.setSystem(true);
              roles.saveAndFlush(admin);
              sync.synchronizeCurrentTenant();
              entityManager.clear();
              assertThat(
                      roles.findByName("Admin").orElseThrow().getPermissions().stream()
                          .map(Permission::getCode))
                  .contains("USER_MANAGE")
                  .doesNotContain("BILL_MANAGE");
            });
  }

  @Test
  void aCustomRoleAndItsCustomPermissionAreNotOverwritten() {
    TenantContext.set(A);
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              status.setRollbackOnly();
              var permission = new Permission();
              permission.setCode("SYNTHETIC_CUSTOM_" + UUID.randomUUID());
              permission.setModule("synthetic");
              permissions.saveAndFlush(permission);
              var manager = new Role();
              manager.setName("Manager");
              manager.grantPermission(permission);
              roles.saveAndFlush(manager);
              var managerId = manager.getId();
              var customCode = permission.getCode();
              sync.synchronizeCurrentTenant();
              entityManager.clear();
              var saved = roles.findByName("Manager").orElseThrow();
              assertThat(saved.getId()).isEqualTo(managerId);
              assertThat(saved.isSystem()).isFalse();
              assertThat(saved.getPermissions().stream().map(Permission::getCode))
                  .containsExactly(customCode);
            });
  }

  @Test
  void missingTenantContextCannotStartSynchronisation() {
    assertThatThrownBy(sync::synchronizeCurrentTenant)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("requires tenant context");
  }

  @Test
  void aRolledBackSyncLeavesTheExistingPermissionAndRoleSetsUntouched() {
    TenantContext.set(A);
    var before = snapshot();
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              sync.synchronizeCurrentTenant();
              assertThat(permissions.findByCode("BILL_MANAGE")).isPresent();
              status.setRollbackOnly();
            });
    assertThat(snapshot()).containsExactlyInAnyOrderEntriesOf(before);
  }

  @Test
  void anotherTenantsExistingPermissionCannotBeReusedByTheCurrentTenant() {
    var other = UUID.fromString("04b270a3-35dc-4bbd-b709-0942f6f7ef93");
    var created = new AtomicBoolean();
    TenantContext.set(other);
    var otherId =
        new TransactionTemplate(transactions)
            .execute(
                status ->
                    permissions
                        .findByCode("BILL_MANAGE")
                        .map(Permission::getId)
                        .orElseGet(
                            () -> {
                              var permission = new Permission();
                              permission.setCode("BILL_MANAGE");
                              permission.setModule("finance");
                              created.set(true);
                              return permissions.saveAndFlush(permission).getId();
                            }));
    try {
      TenantContext.set(A);
      new TransactionTemplate(transactions)
          .executeWithoutResult(
              status -> {
                status.setRollbackOnly();
                sync.synchronizeCurrentTenant();
                var own = permissions.findByCode("BILL_MANAGE").orElseThrow();
                assertThat(own.getTenantId()).isEqualTo(A);
                assertThat(own.getId()).isNotEqualTo(otherId);
                assertThat(permissions.findById(otherId)).isEmpty();
              });
      TenantContext.set(other);
      new TransactionTemplate(transactions)
          .executeWithoutResult(
              status ->
                  assertThat(permissions.findById(otherId).orElseThrow().getTenantId())
                      .isEqualTo(other));
    } finally {
      TenantContext.set(other);
      if (created.get()) {
        new TransactionTemplate(transactions)
            .executeWithoutResult(
                status -> {
                  permissions.deleteById(otherId);
                  permissions.flush();
                });
      }
    }
  }

  @Test
  void concurrentCommittedSyncsKeepOneCatalogueAndOneSetOfDefaultRoles() throws Exception {
    TenantContext.set(A);
    var before = snapshot();
    var firstRows = new AtomicReference<Map<String, Set<UUID>>>();
    var firstReady = new CountDownLatch(1);
    var secondStarted = new CountDownLatch(1);
    var releaseFirst = new CountDownLatch(1);
    var workers = Executors.newFixedThreadPool(2);
    try {
      var first =
          workers.submit(
              () -> {
                TenantContext.set(A);
                try {
                  return new TransactionTemplate(transactions)
                      .execute(
                          status -> {
                            sync.synchronizeCurrentTenant();
                            firstRows.set(snapshot());
                            firstReady.countDown();
                            try {
                              if (!releaseFirst.await(15, TimeUnit.SECONDS)) {
                                throw new IllegalStateException(
                                    "Synthetic concurrency release timed out");
                              }
                            } catch (InterruptedException interrupted) {
                              Thread.currentThread().interrupt();
                              throw new IllegalStateException(interrupted);
                            }
                            return snapshot();
                          });
                } finally {
                  TenantContext.clear();
                }
              });
      assertThat(firstReady.await(15, TimeUnit.SECONDS)).isTrue();
      var second =
          workers.submit(
              () -> {
                TenantContext.set(A);
                try {
                  secondStarted.countDown();
                  return new TransactionTemplate(transactions)
                      .execute(
                          status -> {
                            sync.synchronizeCurrentTenant();
                            return snapshot();
                          });
                } finally {
                  TenantContext.clear();
                }
              });
      assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
      assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS))
          .isInstanceOf(TimeoutException.class);
      releaseFirst.countDown();
      assertThat(second.get(15, TimeUnit.SECONDS))
          .containsExactlyInAnyOrderEntriesOf(first.get(15, TimeUnit.SECONDS));
    } finally {
      releaseFirst.countDown();
      workers.shutdown();
      assertThat(workers.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
      if (firstRows.get() != null) {
        TenantContext.set(A);
        new TransactionTemplate(transactions)
            .executeWithoutResult(
                status -> {
                  var addedRoles = new HashSet<>(firstRows.get().get("roles"));
                  addedRoles.removeAll(before.get("roles"));
                  roles.deleteAllById(addedRoles);
                  roles.flush();
                  var addedPermissions = new HashSet<>(firstRows.get().get("permissions"));
                  addedPermissions.removeAll(before.get("permissions"));
                  permissions.deleteAllById(addedPermissions);
                  permissions.flush();
                });
      }
    }
    assertThat(snapshot()).containsExactlyInAnyOrderEntriesOf(before);
  }

  private Map<String, Set<UUID>> snapshot() {
    return new TransactionTemplate(transactions)
        .execute(
            status ->
                Map.of(
                    "permissions",
                        permissions.findAll().stream()
                            .map(Permission::getId)
                            .collect(Collectors.toSet()),
                    "roles",
                        roles.findAll().stream().map(Role::getId).collect(Collectors.toSet())));
  }
}
