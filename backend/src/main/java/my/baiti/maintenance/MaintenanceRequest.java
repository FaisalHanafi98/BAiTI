package my.baiti.maintenance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import my.baiti.common.TenantAwareBaseEntity;
import my.baiti.people.User;
import my.baiti.property.Unit;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "maintenance_requests")
public class MaintenanceRequest extends TenantAwareBaseEntity {

  public enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
  }

  public enum Status {
    PENDING,
    APPROVED,
    IN_PROGRESS,
    COMPLETED,
    REJECTED
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "unit_id", nullable = false)
  private Unit unit;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "requested_by", nullable = false)
  private User requestedBy;

  @Column(name = "description", nullable = false)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "priority", nullable = false)
  private Priority priority = Priority.MEDIUM;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status = Status.PENDING;
}
