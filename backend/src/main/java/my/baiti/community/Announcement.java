package my.baiti.community;

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
import my.baiti.people.User;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "announcements")
public class Announcement extends TenantAwareBaseEntity {

  public enum TargetAudience {
    ALL,
    OWNERS,
    TENANTS
  }

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "content", nullable = false)
  private String content;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "posted_by", nullable = false)
  private User postedBy;

  @Column(name = "attachment")
  private String attachment;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_audience", nullable = false)
  private TargetAudience targetAudience = TargetAudience.ALL;

  @Column(name = "posted_at", nullable = false)
  private Instant postedAt = Instant.now();
}
