package my.baiti.people;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import my.baiti.common.TenantAwareBaseEntity;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "family_members")
public class FamilyMember extends TenantAwareBaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resident_id", nullable = false)
  private Resident resident;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "relationship")
  private String relationship;

  @Column(name = "age")
  private Short age;

  @Column(name = "contact_no")
  private String contactNo;
}
