package my.baiti.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import my.baiti.config.DataSourceConfig;
import my.baiti.config.JpaConfig;
import my.baiti.property.Block;
import my.baiti.property.BlockRepository;
import my.baiti.security.TenantPrincipal;
import my.baiti.testing.NativePostgresTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({DataSourceConfig.class, JpaConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class HibernateTenantIT extends NativePostgresTestSupport {

  @Autowired private BlockRepository blocks;
  @Autowired private EntityManager entityManager;
  @Autowired private PlatformTransactionManager transactionManager;

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
    TenantContext.clear();
  }

  @Test
  void persistedCreationAndUpdateRetainTheCorrectAuthenticatedActors() {
    var tenant = UUID.fromString("ad8cdb3d-885a-4f0d-b678-2331ce582805");
    var creator = UUID.randomUUID();
    var editor = UUID.randomUUID();
    TenantContext.set(tenant);
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status -> {
              status.setRollbackOnly();
              authenticate(creator, tenant);
              var block = new Block();
              block.setName("Synthetic audited block " + UUID.randomUUID());
              var id = blocks.saveAndFlush(block).getId();
              entityManager.clear();
              var saved = blocks.findById(id).orElseThrow();
              assertThat(saved.getCreatedBy()).isEqualTo(creator);
              assertThat(saved.getUpdatedBy()).isEqualTo(creator);
              authenticate(editor, tenant);
              saved.setName("Synthetic updated block " + id);
              blocks.saveAndFlush(saved);
              entityManager.clear();
              var updated = blocks.findById(id).orElseThrow();
              assertThat(updated.getCreatedBy()).isEqualTo(creator);
              assertThat(updated.getUpdatedBy()).isEqualTo(editor);
            });
  }

  private static void authenticate(UUID user, UUID tenant) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                new TenantPrincipal(user, tenant, Set.of("Admin"), Set.of()), null, List.of()));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"ad8cdb3d-885a-4f0d-b678-2331ce582805", "04b270a3-35dc-4bbd-b709-0942f6f7ef93"})
  void productionResolverAssignsTheCurrentTenantWithoutTheFoundationFixture(String value) {
    var tenant = UUID.fromString(value);
    TenantContext.set(tenant);
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status -> {
              status.setRollbackOnly();
              var block = new Block();
              block.setName("Synthetic production resolver " + UUID.randomUUID());
              var id = blocks.saveAndFlush(block).getId();
              entityManager.clear();
              assertThat(blocks.findById(id).orElseThrow().getTenantId()).isEqualTo(tenant);
              assertThat(
                      entityManager
                          .createNativeQuery("SELECT public.baiti_current_tenant()", UUID.class)
                          .getSingleResult())
                  .isEqualTo(tenant);
            });
  }

  @Test
  void absentContextCannotWriteThroughHibernate() {
    assertThatThrownBy(
            () ->
                new TransactionTemplate(transactionManager)
                    .executeWithoutResult(
                        status -> {
                          status.setRollbackOnly();
                          var block = new Block();
                          block.setName("Synthetic missing tenant");
                          blocks.saveAndFlush(block);
                        }))
        .rootCause()
        .isInstanceOfSatisfying(
            SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("42501"));
  }
}
