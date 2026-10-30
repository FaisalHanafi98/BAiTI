package my.baiti.people;

import java.util.Optional;
import java.util.UUID;

public interface UserAuthentication {
  Optional<UserIdentity> verifyPassword(String email, String password);

  Optional<UserIdentity> findActiveIdentity(UUID userId);
}
