package my.baiti.property;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UnitRepository extends JpaRepository<Unit, UUID>, JpaSpecificationExecutor<Unit> {
  @Override
  @EntityGraph(attributePaths = "block")
  Page<Unit> findAll(Specification<Unit> spec, Pageable pageable);

  Optional<Unit> findByBlockIdAndUnitNo(UUID blockId, String unitNo);

  List<Unit> findByBlockId(UUID blockId);

  List<Unit> findByStatus(Unit.Status status);
}
