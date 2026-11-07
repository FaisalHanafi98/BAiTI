package my.baiti.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import my.baiti.common.ResourceNotFoundException;
import my.baiti.people.ResidentAccess;
import my.baiti.tenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class ResidentScopeGuardTest {

  @AfterEach
  void clearContexts() {
    TenantContext.clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  void aResidentCanUseTheirOwnRecordButCannotUseAnotherResidentsRecord() {
    var user = UUID.randomUUID();
    var tenant = UUID.randomUUID();
    var own = UUID.randomUUID();
    var guard =
        new ResidentScopeGuard(
            new ResidentAccess() {
              @Override
              public boolean ownsResident(UUID candidateUser, UUID resident) {
                return user.equals(candidateUser) && own.equals(resident);
              }

              @Override
              public boolean hasActiveUnit(UUID candidateUser, UUID unit) {
                return false;
              }
            });
    TenantContext.set(tenant);
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                new TenantPrincipal(user, tenant, Set.of("Resident"), Set.of()), null, List.of()));
    assertThatCode(() -> guard.requireResident(own)).doesNotThrowAnyException();
    assertThatThrownBy(() -> guard.requireResident(UUID.randomUUID()))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Resource not found");
  }
}
