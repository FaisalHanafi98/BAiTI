package my.baiti.system;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
  List<Notification> findByUserIdAndIsReadFalse(UUID userId);

  List<Notification> findByUserId(UUID userId);
}
