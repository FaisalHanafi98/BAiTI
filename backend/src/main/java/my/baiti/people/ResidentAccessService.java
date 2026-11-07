package my.baiti.people;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ResidentAccessService implements ResidentAccess {

  private final ResidentRepository residents;

  public ResidentAccessService(ResidentRepository residents) {
    this.residents = residents;
  }

  @Override
  public boolean ownsResident(UUID userId, UUID residentId) {
    return userId != null
        && residentId != null
        && residents.existsByIdAndUserId(residentId, userId);
  }

  @Override
  public boolean hasActiveUnit(UUID userId, UUID unitId) {
    return userId != null
        && unitId != null
        && residents.existsByUserIdAndUnitIdAndStatus(userId, unitId, Resident.Status.ACTIVE);
  }
}
