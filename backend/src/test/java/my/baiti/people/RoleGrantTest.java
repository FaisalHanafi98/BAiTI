package my.baiti.people;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoleGrantTest {

  @Test
  void permissionFromAnotherTenantIsRejectedBeforeChangingGrants() {
    var role = role();
    var foreign = permission(UUID.randomUUID());

    assertThatIllegalArgumentException().isThrownBy(() -> role.grantPermission(foreign));
    assertThat(role.getPermissions()).isEmpty();
  }

  @Test
  void permissionWithoutPersistentIdentityIsRejected() {
    var role = role();
    var permission = permission(role.getTenantId());
    permission.setId(null);

    assertThatIllegalArgumentException().isThrownBy(() -> role.grantPermission(permission));
    assertThat(role.getPermissions()).isEmpty();
  }

  @Test
  void duplicateGrantDoesNotCreateASecondLink() {
    var role = role();
    var permission = permission(role.getTenantId());

    assertThat(role.grantPermission(permission)).isTrue();
    assertThat(role.grantPermission(permission)).isFalse();
    assertThat(role.getPermissions()).containsExactly(permission);
  }

  @Test
  void permissionProjectionCannotBypassGrantValidation() {
    var role = role();

    assertThatThrownBy(() -> role.getPermissions().add(permission(UUID.randomUUID())))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void revokingAGrantRemovesOnlyThatPermission() {
    var role = role();
    var first = permission(role.getTenantId());
    var second = permission(role.getTenantId());
    role.grantPermission(first);
    role.grantPermission(second);

    assertThat(role.revokePermission(first)).isTrue();
    assertThat(role.revokePermission(first)).isFalse();
    assertThat(role.getPermissions()).containsExactly(second);
  }

  @Test
  void revocationRejectsAForeignTenantPermissionWithoutChangingGrants() {
    var role = role();
    var first = permission(role.getTenantId());
    role.grantPermission(first);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> role.revokePermission(permission(UUID.randomUUID())));
    assertThat(role.getPermissions()).containsExactly(first);
  }

  private static Role role() {
    var role = new Role();
    role.setId(UUID.randomUUID());
    role.setTenantId(UUID.randomUUID());
    return role;
  }

  private static Permission permission(UUID tenant) {
    var permission = new Permission();
    permission.setId(UUID.randomUUID());
    permission.setTenantId(tenant);
    return permission;
  }
}
