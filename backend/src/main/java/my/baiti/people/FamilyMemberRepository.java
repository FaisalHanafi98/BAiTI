package my.baiti.people;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, UUID> {
  List<FamilyMember> findByResidentId(UUID residentId);
}
