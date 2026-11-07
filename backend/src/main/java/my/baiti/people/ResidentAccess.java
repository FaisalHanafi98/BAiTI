package my.baiti.people;

import java.util.UUID;

public interface ResidentAccess {
  boolean ownsResident(UUID userId, UUID residentId);

  boolean hasActiveUnit(UUID userId, UUID unitId);
}
