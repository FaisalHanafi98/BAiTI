package my.baiti.finance;

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
    name = "payment_methods",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_payment_methods_tenant_name",
            columnNames = {"tenant_id", "name"}))
public class PaymentMethod extends TenantAwareBaseEntity {

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "is_active", nullable = false)
  private boolean isActive = true;
}
