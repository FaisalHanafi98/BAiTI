package my.baiti.visitors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import my.baiti.common.TenantAwareBaseEntity;
import my.baiti.people.Resident;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "visitors")
public class Visitor extends TenantAwareBaseEntity {

  public enum Status {
    CHECKED_IN,
    CHECKED_OUT
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resident_id", nullable = false)
  private Resident resident;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pre_registration_id")
  private VisitorPreRegistration preRegistration;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "vehicle_no")
  private String vehicleNo;

  @Column(name = "visit_reason")
  private String visitReason;

  @Column(name = "entry_time")
  private Instant entryTime;

  @Column(name = "exit_time")
  private Instant exitTime;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status = Status.CHECKED_IN;
}
