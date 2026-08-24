package my.baiti.facilities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    name = "facilities",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_facilities_tenant_name",
            columnNames = {"tenant_id", "name"}))
public class Facility extends TenantAwareBaseEntity {

  public enum Status {
    AVAILABLE,
    MAINTENANCE,
    CLOSED
  }

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "location")
  private String location;

  @Column(name = "capacity")
  private Integer capacity;

  @Column(name = "rules")
  private String rules;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status = Status.AVAILABLE;
}
