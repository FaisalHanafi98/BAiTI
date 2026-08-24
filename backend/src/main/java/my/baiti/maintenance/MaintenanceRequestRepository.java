package my.baiti.maintenance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, UUID> {
  List<MaintenanceRequest> findByUnitId(UUID unitId);

  List<MaintenanceRequest> findByStatus(MaintenanceRequest.Status status);
}
