package my.baiti.system;

import java.util.UUID;

public record PeopleActivity(UUID tenantId, UUID actorId, Action action, UUID resourceId) {
  public enum Action {
    USER_CREATED,
    USER_ROLE_CHANGED,
    USER_DEACTIVATED
  }
}
