package my.baiti.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NativePostgresPreflightIT {

  @Test
  void currentOwnedTargetPassesRealCatalogAndRolePreflight() {
    var config = NativePostgresConfig.from(System.getenv());

    assertThatCode(() -> NativePostgresPreflight.verify(config)).doesNotThrowAnyException();
  }

  @Test
  void incorrectOwnershipMarkerIsRejectedWithoutChangingSchema() throws Exception {
    var config = NativePostgresConfig.from(System.getenv());
    long originalRelations = relationCount(config);
    var wrongEnvironment = new HashMap<>(System.getenv());
    wrongEnvironment.put("TEST_DB_OWNER_TOKEN", UUID.randomUUID().toString());

    assertThatIllegalStateException()
        .isThrownBy(
            () -> NativePostgresPreflight.verify(NativePostgresConfig.from(wrongEnvironment)))
        .withMessageContaining("ownership marker");
    assertThat(relationCount(config)).isEqualTo(originalRelations);
  }

  private static long relationCount(NativePostgresConfig config) throws Exception {
    try (var connection = config.openAppConnection();
        var statement =
            connection.prepareStatement(
                "SELECT count(*) FROM pg_class WHERE relnamespace = 'public'::regnamespace");
        var result = statement.executeQuery()) {
      assertThat(result.next()).isTrue();
      return result.getLong(1);
    }
  }
}
