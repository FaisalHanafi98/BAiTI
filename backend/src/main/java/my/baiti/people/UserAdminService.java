package my.baiti.people;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import my.baiti.common.ResourceNotFoundException;
import my.baiti.security.RefreshTokenService;
import my.baiti.system.PeopleActivity;
import my.baiti.tenancy.TenantContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserAdminService {
  private final UserRepository users;
  private final RoleRepository roles;
  private final PasswordEncoder passwords;
  private final ApplicationEventPublisher events;
  private final AuditorAware<UUID> auditor;
  private final RefreshTokenService refresh;
  private final SecureRandom random = new SecureRandom();

  public UserAdminService(
      UserRepository users,
      RoleRepository roles,
      PasswordEncoder passwords,
      ApplicationEventPublisher events,
      @Qualifier("auditorProvider") AuditorAware<UUID> auditor,
      RefreshTokenService refresh) {
    this.users = users;
    this.roles = roles;
    this.passwords = passwords;
    this.events = events;
    this.auditor = auditor;
    this.refresh = refresh;
  }

  public UserResponse get(UUID id) {
    return UserResponse.from(users.findById(id).orElseThrow(ResourceNotFoundException::new));
  }

  public PeoplePage<UserResponse> list(Pageable page) {
    return PeoplePage.from(
        users
            .findAll(PeoplePage.bounded(page, Set.of("name", "email", "status")))
            .map(UserResponse::from));
  }

  public PeoplePage<RoleResponse> roles(Pageable page) {
    return PeoplePage.from(
        roles.findAll(PeoplePage.bounded(page, Set.of("name"))).map(RoleResponse::from));
  }

  @Transactional
  public UserResponse assignRole(UUID id, AssignRoleRequest request) {
    var user = users.findForAdministration(id).orElseThrow(ResourceNotFoundException::new);
    var role = roles.findById(request.roleId()).orElseThrow(ResourceNotFoundException::new);
    if (user.getRole().getId().equals(role.getId())) {
      return UserResponse.from(user);
    }
    user.setRole(role);
    users.flush();
    var response = UserResponse.from(user);
    refresh.revokeUser(id);
    record(PeopleActivity.Action.USER_ROLE_CHANGED, id);
    return response;
  }

  @Transactional
  public void deactivate(UUID id) {
    var user = users.findForAdministration(id).orElseThrow(ResourceNotFoundException::new);
    if (user.getStatus() == User.Status.INACTIVE) return;
    user.setStatus(User.Status.INACTIVE);
    users.flush();
    refresh.revokeUser(id);
    record(PeopleActivity.Action.USER_DEACTIVATED, id);
  }

  @Transactional
  public CreatedUserResponse create(CreateUserRequest request) {
    var role = roles.findById(request.roleId()).orElseThrow(ResourceNotFoundException::new);
    byte[] bytes = new byte[24];
    random.nextBytes(bytes);
    var temporaryPassword = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    var user = new User();
    user.setName(request.name().strip());
    user.setEmail(request.email());
    user.setRole(role);
    user.setPasswordHash(passwords.encode(temporaryPassword));
    user.setMustChangePassword(true);
    users.saveAndFlush(user);
    record(PeopleActivity.Action.USER_CREATED, user.getId());
    return new CreatedUserResponse(UserResponse.from(user), temporaryPassword);
  }

  private void record(PeopleActivity.Action action, UUID id) {
    events.publishEvent(
        new PeopleActivity(
            TenantContext.current().orElseThrow(),
            auditor.getCurrentAuditor().orElseThrow(),
            action,
            id));
  }
}
