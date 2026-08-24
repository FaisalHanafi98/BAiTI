package my.baiti.people;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResidentRepository extends JpaRepository<Resident, UUID> {
  List<Resident> findByUnitId(UUID unitId);

  Optional<Resident> findByUserIdAndStatus(UUID userId, Resident.Status status);
}
