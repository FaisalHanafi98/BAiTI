package my.baiti.people;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasAnyAuthority('USER_MANAGE', 'ROLE_MANAGE')")
public class RoleController {
  private final UserAdminService users;

  public RoleController(UserAdminService users) {
    this.users = users;
  }

  @GetMapping
  public ResponseEntity<PeoplePage<RoleResponse>> listRoles(
      @ParameterObject @PageableDefault(size = 20) Pageable page) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(users.roles(page));
  }
}
