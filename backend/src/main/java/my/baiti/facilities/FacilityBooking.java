package my.baiti.facilities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import my.baiti.common.TenantAwareBaseEntity;
import my.baiti.people.Resident;
import my.baiti.people.User;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "facility_bookings")
public class FacilityBooking extends TenantAwareBaseEntity {

  public enum Status {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "facility_id", nullable = false)
  private Facility facility;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resident_id", nullable = false)
  private Resident resident;

  @Column(name = "booking_date", nullable = false)
  private LocalDate bookingDate;

  @Column(name = "time_slot", nullable = false)
  private String timeSlot;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status = Status.PENDING;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "approved_by")
  private User approvedBy;
}
