package my.baiti.people;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAuthority('USER_MANAGE')")
public class UserAdminController {
  private final UserAdminService users;

  public UserAdminController(UserAdminService users) {
    this.users = users;
  }

  @GetMapping
  public ResponseEntity<PeoplePage<UserResponse>> listUsers(
      @ParameterObject @PageableDefault(size = 20) Pageable page) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(users.list(page));
  }

  @PutMapping("/{id}/role")
  public ResponseEntity<UserResponse> assignUserRole(
      @PathVariable UUID id, @Valid @RequestBody AssignRoleRequest request) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(users.assignRole(id, request));
  }

  @PostMapping("/{id}/deactivate")
  public ResponseEntity<Void> deactivateUser(@PathVariable UUID id) {
    users.deactivate(id);
    return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(users.get(id));
  }

  @PostMapping
  public ResponseEntity<CreatedUserResponse> createUser(
      @Valid @RequestBody CreateUserRequest request) {
    var created = users.create(request);
    return ResponseEntity.created(URI.create("/api/users/" + created.user().id()))
        .cacheControl(CacheControl.noStore())
        .body(created);
  }
}
