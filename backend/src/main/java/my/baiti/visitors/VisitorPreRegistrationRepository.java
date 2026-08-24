package my.baiti.visitors;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitorPreRegistrationRepository
    extends JpaRepository<VisitorPreRegistration, UUID> {
  Optional<VisitorPreRegistration> findByApprovalCode(String approvalCode);

  List<VisitorPreRegistration> findByResidentId(UUID residentId);

  List<VisitorPreRegistration> findByVisitDate(LocalDate visitDate);
}
