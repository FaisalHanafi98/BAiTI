package my.baiti.complaints;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintCategoryRepository extends JpaRepository<ComplaintCategory, UUID> {
  Optional<ComplaintCategory> findByName(String name);
}
