package my.baiti.system;

import java.time.Instant;
import java.util.UUID;

public record TenantActivity(
    UUID tenantId, UUID actorId, Action action, String address, Instant occurredAt) {
  public enum Action {
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    TOKEN_REFRESHED,
    LOGOUT,
    PASSWORD_CHANGED
  }
}
