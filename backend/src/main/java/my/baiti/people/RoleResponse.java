package my.baiti.people;

import java.util.UUID;

public record RoleResponse(UUID id, String name, String description, boolean system) {
  static RoleResponse from(Role role) {
    return new RoleResponse(role.getId(), role.getName(), role.getDescription(), role.isSystem());
  }
}
