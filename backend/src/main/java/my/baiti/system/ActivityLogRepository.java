package my.baiti.system;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {
  List<ActivityLog> findByUserId(UUID userId);

  List<ActivityLog> findByAction(String action);
}
