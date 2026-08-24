package my.baiti.people;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import my.baiti.common.TenantAwareBaseEntity;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "permissions",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_permissions_tenant_code",
            columnNames = {"tenant_id", "code"}))
public class Permission extends TenantAwareBaseEntity {

  @Column(name = "code", nullable = false)
  private String code;

  @Column(name = "module", nullable = false)
  private String module;

  @Column(name = "description")
  private String description;
}
