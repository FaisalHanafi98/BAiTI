package my.baiti.finance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import my.baiti.common.TenantAwareBaseEntity;
import my.baiti.property.Unit;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "bills",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_bills_unit_month_type",
            columnNames = {"tenant_id", "unit_id", "billing_month", "bill_type"}))
public class Bill extends TenantAwareBaseEntity {

  public enum Status {
    UNPAID,
    PAID,
    OVERDUE,
    CANCELLED
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "unit_id", nullable = false)
  private Unit unit;

  @Column(name = "billing_month", nullable = false)
  private LocalDate billingMonth;

  @Column(name = "bill_type", nullable = false)
  private String billType = "MAINTENANCE_FEE";

  @Column(name = "description")
  private String description;

  @Column(name = "amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency = "MYR";

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status = Status.UNPAID;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Column(name = "cancelled_reason")
  private String cancelledReason;
}
