package my.baiti.system;

import java.util.UUID;

public record PropertyActivity(UUID tenantId, UUID actorId, Action action, UUID resourceId) {
  public enum Action {
    BLOCK_CREATED,
    BLOCK_UPDATED,
    BLOCK_DELETED,
    UNIT_CREATED,
    UNIT_UPDATED,
    UNIT_DELETED
  }
}
