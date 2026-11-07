package my.baiti.tenancy.admin;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import my.baiti.people.DefaultRoleGrants;
import my.baiti.people.PermissionCatalog;
import my.baiti.security.PlatformPrincipal;
import my.baiti.tenancy.TenantContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@UsesPrivilegedDataSource
public class TenantOnboardingService {
  private final JdbcTemplate platform;
  private final PasswordEncoder passwords;
  private final TransactionTemplate transaction;

  public TenantOnboardingService(
      @Qualifier("platformJdbcTemplate") JdbcTemplate platform, PasswordEncoder passwords) {
    this.platform = platform;
    this.passwords = passwords;
    this.transaction =
        new TransactionTemplate(new JdbcTransactionManager(platform.getDataSource()));
  }

  public CreatedTenant create(
      PlatformPrincipal actor, TenantRegistration registration, String address) {
    requirePlatform(actor);
    return transaction.execute(
        status -> {
          if (platform.queryForObject("SELECT public.baiti_current_tenant()", UUID.class) != null) {
            throw new IllegalStateException("Platform connection already has a tenant scope");
          }
          var tenant = UUID.randomUUID();
          int inserted =
              platform.update(
                  "INSERT INTO tenants (id, slug, name) VALUES (?, ?, ?) ON CONFLICT (slug) DO NOTHING",
                  tenant,
                  registration.slug(),
                  registration.name());
          if (inserted == 0) {
            throw new SlugConflict();
          }
          platform.queryForObject(
              "SELECT set_config('app.current_tenant', ?, true)", String.class, tenant.toString());
          var roles = seedRoles(tenant);
          var permissions = seedPermissions(tenant);
          var grants = new ArrayList<Object[]>();
          DefaultRoleGrants.all()
              .forEach(
                  (role, codes) ->
                      codes.forEach(
                          code ->
                              grants.add(
                                  new Object[] {roles.get(role), permissions.get(code), tenant})));
          platform.batchUpdate(
              "INSERT INTO roles_permissions (role_id, permission_id, tenant_id) VALUES (?, ?, ?)",
              grants);
          var admin = UUID.randomUUID();
          platform.update(
              "INSERT INTO users (id, tenant_id, name, email, password_hash, role_id, must_change_password)"
                  + " VALUES (?, ?, ?, ?, ?, ?, true)",
              admin,
              tenant,
              registration.adminName(),
              registration.adminEmail(),
              passwords.encode(registration.initialPassword()),
              roles.get("Admin"));
          platform.update(
              "INSERT INTO activity_logs (tenant_id, action, detail, ip_address)"
                  + " VALUES (?, 'TENANT_CREATED', jsonb_build_object('platformAdminId', CAST(? AS text),"
                  + " 'adminUserId', CAST(? AS text)), CAST(? AS inet))",
              tenant,
              actor.adminId(),
              admin,
              address);
          platform.queryForObject(
              "SELECT set_config('app.current_tenant', '', true)", String.class);
          return new CreatedTenant(tenant, admin);
        });
  }

  private Map<String, UUID> seedRoles(UUID tenant) {
    var roles = new HashMap<String, UUID>();
    var rows = new ArrayList<Object[]>();
    for (var name : DefaultRoleGrants.all().keySet()) {
      var id = UUID.randomUUID();
      roles.put(name, id);
      rows.add(new Object[] {id, tenant, name});
    }
    platform.batchUpdate(
        "INSERT INTO roles (id, tenant_id, name, is_system) VALUES (?, ?, ?, true)", rows);
    return roles;
  }

  private Map<PermissionCatalog, UUID> seedPermissions(UUID tenant) {
    var permissions = new EnumMap<PermissionCatalog, UUID>(PermissionCatalog.class);
    var rows = new ArrayList<Object[]>();
    for (var code : PermissionCatalog.values()) {
      var id = UUID.randomUUID();
      permissions.put(code, id);
      rows.add(new Object[] {id, tenant, code.name(), code.module()});
    }
    platform.batchUpdate(
        "INSERT INTO permissions (id, tenant_id, code, module) VALUES (?, ?, ?, ?)", rows);
    return permissions;
  }

  public TenantPage list(PlatformPrincipal actor, Pageable pageable) {
    requirePlatform(actor);
    int size = Math.min(pageable.getPageSize(), 100);
    int page = pageable.getPageNumber();
    var order = pageable.getSort().getOrderFor("slug");
    String direction = order != null && order.isDescending() ? " DESC" : " ASC";
    return transaction.execute(
        status -> {
          long total =
              Objects.requireNonNull(
                  platform.queryForObject("SELECT count(*) FROM tenants", Long.class));
          var content =
              platform.query(
                  "SELECT id, slug, name, status FROM tenants ORDER BY slug"
                      + direction
                      + " LIMIT ? OFFSET ?",
                  (row, index) ->
                      new TenantSummary(
                          row.getObject("id", UUID.class),
                          row.getString("slug"),
                          row.getString("name"),
                          row.getString("status")),
                  size,
                  (long) page * size);
          return new TenantPage(content, total, (total + size - 1) / size, page, size);
        });
  }

  private static void requirePlatform(PlatformPrincipal actor) {
    if (actor == null || TenantContext.current().isPresent()) {
      throw new AccessDeniedException("Access denied");
    }
  }

  public record CreatedTenant(UUID tenantId, UUID adminUserId) {}

  public record TenantSummary(UUID id, String slug, String name, String status) {}

  public record TenantPage(
      List<TenantSummary> content, long totalElements, long totalPages, int page, int size) {}

  public static final class SlugConflict extends RuntimeException {}
}
