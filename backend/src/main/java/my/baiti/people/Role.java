package my.baiti.people;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import my.baiti.common.TenantAwareBaseEntity;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "roles",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_roles_tenant_name",
            columnNames = {"tenant_id", "name"}))
public class Role extends TenantAwareBaseEntity {

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "is_system", nullable = false)
  private boolean isSystem = false;

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RolePermission> grants = new ArrayList<>();

  public Set<Permission> getPermissions() {
    return grants.stream()
        .map(RolePermission::getPermission)
        .collect(Collectors.toUnmodifiableSet());
  }

  public boolean grantPermission(Permission permission) {
    validatePermissionScope(permission);
    if (grants.stream()
        .anyMatch(grant -> Objects.equals(grant.getPermission().getId(), permission.getId()))) {
      return false;
    }
    grants.add(new RolePermission(this, permission));
    return true;
  }

  public boolean revokePermission(Permission permission) {
    validatePermissionScope(permission);
    return grants.removeIf(
        grant -> Objects.equals(grant.getPermission().getId(), permission.getId()));
  }

  private void validatePermissionScope(Permission permission) {
    if (permission == null || permission.getId() == null || permission.getTenantId() == null) {
      throw new IllegalArgumentException(
          "Permission must have persistent identity and tenant scope");
    }
    if (getTenantId() != null && !getTenantId().equals(permission.getTenantId())) {
      throw new IllegalArgumentException("Role and permission must belong to the same tenant");
    }
  }
}
