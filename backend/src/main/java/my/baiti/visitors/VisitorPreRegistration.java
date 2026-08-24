package my.baiti.visitors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import my.baiti.common.TenantAwareBaseEntity;
import my.baiti.people.Resident;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "visitor_pre_registrations",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_visitor_pre_reg_approval_code",
            columnNames = {"tenant_id", "approval_code"}))
public class VisitorPreRegistration extends TenantAwareBaseEntity {

  public enum Status {
    PENDING,
    USED,
    EXPIRED,
    CANCELLED
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resident_id", nullable = false)
  private Resident resident;

  @Column(name = "visitor_name", nullable = false)
  private String visitorName;

  @Column(name = "visit_date", nullable = false)
  private LocalDate visitDate;

  @Column(name = "approval_code", nullable = false)
  private String approvalCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status = Status.PENDING;
}
