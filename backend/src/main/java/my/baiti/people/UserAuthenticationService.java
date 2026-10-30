package my.baiti.people;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAuthenticationService implements UserAuthentication {

  private final UserRepository users;
  private final PasswordEncoder passwords;
  private final Clock clock;
  private final String dummyHash;

  public UserAuthenticationService(UserRepository users, PasswordEncoder passwords, Clock clock) {
    this.users = users;
    this.passwords = passwords;
    this.clock = clock;
    dummyHash = passwords.encode(UUID.randomUUID().toString());
  }

  @Override
  @Transactional
  public Optional<UserIdentity> verifyPassword(String email, String password) {
    var user = users.findByEmail(email);
    boolean matches =
        passwords.matches(password, user.map(User::getPasswordHash).orElse(dummyHash));
    if (!matches || user.isEmpty() || user.orElseThrow().getStatus() != User.Status.ACTIVE) {
      return Optional.empty();
    }
    var active = user.orElseThrow();
    active.setLastLoginAt(clock.instant());
    return Optional.of(identity(active));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UserIdentity> findActiveIdentity(UUID id) {
    return users
        .findById(id)
        .filter(user -> user.getStatus() == User.Status.ACTIVE)
        .map(this::identity);
  }

  private UserIdentity identity(User user) {
    return new UserIdentity(
        user.getId(),
        user.getTenantId(),
        user.getName(),
        user.getEmail(),
        user.getRole().getName(),
        user.getRole().getPermissions().stream()
            .map(Permission::getCode)
            .collect(Collectors.toUnmodifiableSet()),
        user.isMustChangePassword());
  }
}
