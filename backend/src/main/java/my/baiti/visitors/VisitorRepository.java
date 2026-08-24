package my.baiti.visitors;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitorRepository extends JpaRepository<Visitor, UUID> {
  List<Visitor> findByResidentId(UUID residentId);

  List<Visitor> findByStatus(Visitor.Status status);
}
