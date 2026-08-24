package my.baiti.system;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import my.baiti.common.TenantAwareBaseEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "activity_logs")
public class ActivityLog extends TenantAwareBaseEntity {

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "action", nullable = false)
  private String action;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "detail", columnDefinition = "jsonb")
  private String detail;

  @Column(name = "ip_address")
  private String ipAddress;
}
