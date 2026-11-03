package my.baiti.people;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import my.baiti.tenancy.TenantContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissionSyncService implements PermissionSynchronizer {

  private final PermissionRepository permissions;
  private final RoleRepository roles;
  private final JdbcTemplate jdbc;

  public PermissionSyncService(
      PermissionRepository permissions,
      RoleRepository roles,
      @Qualifier("appDataSource") DataSource source) {
    this.permissions = permissions;
    this.roles = roles;
    jdbc = new JdbcTemplate(source);
    jdbc.setQueryTimeout(10);
  }

  @Override
  @Transactional
  public void synchronizeCurrentTenant() {
    var tenant =
        TenantContext.current()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Permission synchronisation requires tenant context"));
    jdbc.query("SELECT pg_advisory_xact_lock(?, ?)", (row, index) -> 0, 0xBA171, tenant.hashCode());
    var stored =
        permissions.findAll().stream()
            .collect(Collectors.toMap(Permission::getCode, permission -> permission));
    var added = EnumSet.noneOf(PermissionCatalog.class);
    var missing = new ArrayList<Permission>();
    for (var entry : PermissionCatalog.values()) {
      if (!stored.containsKey(entry.name())) {
        var permission = new Permission();
        permission.setCode(entry.name());
        permission.setModule(entry.module());
        stored.put(entry.name(), permission);
        missing.add(permission);
        added.add(entry);
      }
    }
    permissions.saveAllAndFlush(missing);
    DefaultRoleGrants.all()
        .forEach(
            (name, grants) -> {
              var existing = roles.findByName(name);
              var role =
                  existing.orElseGet(
                      () -> {
                        var created = new Role();
                        created.setName(name);
                        created.setSystem(true);
                        return created;
                      });
              if (existing.isEmpty() || role.isSystem()) {
                grants.stream()
                    .filter(permission -> existing.isEmpty() || added.contains(permission))
                    .forEach(permission -> role.grantPermission(stored.get(permission.name())));
              }
              roles.saveAndFlush(role);
            });
  }
}
