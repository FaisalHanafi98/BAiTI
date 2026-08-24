package my.baiti.people;

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
import my.baiti.property.Unit;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "residents")
public class Resident extends TenantAwareBaseEntity {

  public enum ResidentType {
    OWNER,
    TENANT
  }

  public enum Status {
    ACTIVE,
    MOVED_OUT
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "unit_id", nullable = false)
  private Unit unit;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "phone")
  private String phone;

  @Column(name = "email", columnDefinition = "citext")
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(name = "resident_type", nullable = false)
  private ResidentType residentType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status = Status.ACTIVE;
}
