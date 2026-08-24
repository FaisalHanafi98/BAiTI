package my.baiti.property;

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
    name = "blocks",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_blocks_tenant_name",
            columnNames = {"tenant_id", "name"}))
public class Block extends TenantAwareBaseEntity {

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description")
  private String description;
}
