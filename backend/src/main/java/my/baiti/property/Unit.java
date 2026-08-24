package my.baiti.property;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
    name = "units",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_units_tenant_block_unit",
            columnNames = {"tenant_id", "block_id", "unit_no"}))
public class Unit extends TenantAwareBaseEntity {

  public enum Status {
    OCCUPIED,
    VACANT
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "block_id", nullable = false)
  private Block block;

  @Column(name = "unit_no", nullable = false)
  private String unitNo;

  @Column(name = "type")
  private String type;

  @Column(name = "floor_no")
  private String floorNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status = Status.VACANT;
}
