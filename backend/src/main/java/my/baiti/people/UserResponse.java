package my.baiti.people;

import java.util.UUID;

public record UserResponse(
    UUID id,
    String name,
    String email,
    UUID roleId,
    String roleName,
    User.Status status,
    boolean mustChangePassword) {
  static UserResponse from(User user) {
    return new UserResponse(
        user.getId(),
        user.getName(),
        user.getEmail(),
        user.getRole().getId(),
        user.getRole().getName(),
        user.getStatus(),
        user.isMustChangePassword());
  }
}
