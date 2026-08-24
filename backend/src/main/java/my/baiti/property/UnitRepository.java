package my.baiti.property;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitRepository extends JpaRepository<Unit, UUID> {
  Optional<Unit> findByBlockIdAndUnitNo(UUID blockId, String unitNo);

  List<Unit> findByBlockId(UUID blockId);

  List<Unit> findByStatus(Unit.Status status);
}
