package my.baiti.people;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import my.baiti.common.TenantScopedBaseEntity;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "roles_permissions")
public class RolePermission extends TenantScopedBaseEntity {

  @EmbeddedId private RolePermissionId id;

  @MapsId("roleId")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "role_id", nullable = false)
  private Role role;

  @MapsId("permissionId")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "permission_id", nullable = false)
  private Permission permission;

  RolePermission(Role role, Permission permission) {
    this.role = role;
    this.permission = permission;
    this.id = new RolePermissionId(role.getId(), permission.getId());
    setTenantId(permission.getTenantId());
  }
}
