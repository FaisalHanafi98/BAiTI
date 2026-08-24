package my.baiti.complaints;

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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "complaint_updates")
public class ComplaintUpdate extends TenantAwareBaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "complaint_id", nullable = false)
  private Complaint complaint;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "message", nullable = false)
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Complaint.Status status;
}
