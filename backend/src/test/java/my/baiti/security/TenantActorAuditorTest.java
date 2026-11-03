package my.baiti.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import my.baiti.config.JpaConfig;
import my.baiti.tenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class TenantActorAuditorTest {

  @AfterEach
  void clearContexts() {
    SecurityContextHolder.clearContext();
    TenantContext.clear();
  }

  @Test
  void aVerifiedPrincipalInTheCurrentTenantSuppliesItsUserId() {
    var user = UUID.randomUUID();
    var tenant = UUID.randomUUID();
    TenantContext.set(tenant);
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                new TenantPrincipal(user, tenant, Set.of("Resident"), Set.of()), null, List.of()));

    assertThat(new JpaConfig().auditorProvider().getCurrentAuditor()).contains(user);
  }

  @Test
  void missingOrForeignTenantContextCannotSupplyAnActor() {
    var principal = new TenantPrincipal(UUID.randomUUID(), UUID.randomUUID(), Set.of(), Set.of());
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
    assertThat(new JpaConfig().auditorProvider().getCurrentAuditor()).isEmpty();
    TenantContext.set(UUID.randomUUID());
    assertThat(new JpaConfig().auditorProvider().getCurrentAuditor()).isEmpty();
  }

  @Test
  void unauthenticatedAndUnrelatedPrincipalsCannotSupplyAnActor() {
    var tenant = UUID.randomUUID();
    TenantContext.set(tenant);
    var auditor = new JpaConfig().auditorProvider();
    assertThat(auditor.getCurrentAuditor()).isEmpty();
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.unauthenticated(
                new TenantPrincipal(UUID.randomUUID(), tenant, Set.of(), Set.of()), null));
    assertThat(auditor.getCurrentAuditor()).isEmpty();
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                UUID.randomUUID().toString(), null, List.of()));
    assertThat(auditor.getCurrentAuditor()).isEmpty();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "synthetic",
                "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
    assertThat(auditor.getCurrentAuditor()).isEmpty();
  }

  @Test
  void clearingSecurityContextDoesNotReuseThePreviousActor() {
    var tenant = UUID.randomUUID();
    TenantContext.set(tenant);
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                new TenantPrincipal(UUID.randomUUID(), tenant, Set.of(), Set.of()),
                null,
                List.of()));
    assertThat(new JpaConfig().auditorProvider().getCurrentAuditor()).isPresent();
    SecurityContextHolder.clearContext();
    assertThat(new JpaConfig().auditorProvider().getCurrentAuditor()).isEmpty();
  }
}
