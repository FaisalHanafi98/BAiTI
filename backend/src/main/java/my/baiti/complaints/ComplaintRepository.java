package my.baiti.complaints;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {
  List<Complaint> findByResidentId(UUID residentId);

  List<Complaint> findByUnitId(UUID unitId);

  List<Complaint> findByStatus(Complaint.Status status);
}
