package my.baiti.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class NativePostgresIdentityTest {

  private static final String DATABASE = "baiti_test_f0";
  private static final String MARKER = "baiti-test:9cc7c390-f8d9-48c8-81a5-ed2b14b8dc82";

  @Test
  void ownedPg16TargetWithUnprivilegedDistinctRolesIsAccepted() {
    assertThatCode(() -> safeIdentity().requireSafe(DATABASE, MARKER)).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @MethodSource("unsafeIdentities")
  void unsafeTargetOrRoleIdentityStopsExecutionBeforeMigration(NativePostgresIdentity identity) {
    assertThatIllegalStateException().isThrownBy(() -> identity.requireSafe(DATABASE, MARKER));
  }

  @Test
  void ownershipDiagnosticsDoNotExposeTheMarkerToken() {
    assertThatIllegalStateException()
        .isThrownBy(() -> safeIdentity().requireSafe(DATABASE, "another-private-marker"))
        .withMessageNotContaining(MARKER)
        .withMessageNotContaining("another-private-marker");
  }

  @Test
  void identityDiagnosticsHideTheOwnershipMarker() {
    assertThat(safeIdentity().toString()).doesNotContain(MARKER);
  }

  private static NativePostgresIdentity safeIdentity() {
    return new NativePostgresIdentity(
        DATABASE,
        160015,
        "baiti_migrator",
        "baiti_migrator",
        MARKER,
        "baiti_migrator",
        false,
        "baiti_app",
        false,
        false,
        false);
  }

  static NativePostgresIdentity[] unsafeIdentities() {
    return new NativePostgresIdentity[] {
      new NativePostgresIdentity(
          "baiti",
          160015,
          "baiti_migrator",
          "baiti_migrator",
          MARKER,
          "baiti_migrator",
          false,
          "baiti_app",
          false,
          false,
          false),
      new NativePostgresIdentity(
          DATABASE,
          150015,
          "baiti_migrator",
          "baiti_migrator",
          MARKER,
          "baiti_migrator",
          false,
          "baiti_app",
          false,
          false,
          false),
      new NativePostgresIdentity(
          DATABASE,
          170015,
          "baiti_migrator",
          "baiti_migrator",
          MARKER,
          "baiti_migrator",
          false,
          "baiti_app",
          false,
          false,
          false),
      new NativePostgresIdentity(
          DATABASE,
          160015,
          "postgres",
          "baiti_migrator",
          MARKER,
          "baiti_migrator",
          false,
          "baiti_app",
          false,
          false,
          false),
      new NativePostgresIdentity(
          DATABASE,
          160015,
          "baiti_migrator",
          "postgres",
          MARKER,
          "baiti_migrator",
          false,
          "baiti_app",
          false,
          false,
          false),
      new NativePostgresIdentity(
          DATABASE,
          160015,
          "baiti_migrator",
          "baiti_migrator",
          null,
          "baiti_migrator",
          false,
          "baiti_app",
          false,
          false,
          false),
      new NativePostgresIdentity(
          DATABASE,
          160015,
          "baiti_migrator",
          "baiti_migrator",
          "different-owner-marker",
          "baiti_migrator",
          false,
          "baiti_app",
          false,
          false,
          false),
      new NativePostgresIdentity(
          DATABASE,
          160015,
          "baiti_migrator",
          "baiti_migrator",
          MARKER,
          "postgres",
          false,
          "baiti_app",
          false,
          false,
          false),
      new NativePostgresIdentity(
          DATABASE,
          160015,
          "baiti_migrator",
          "baiti_migrator",
          MARKER,
          "baiti_migrator",
          true,
          "baiti_app",
          false,
          false,
          false),
      new NativePostgresIdentity(
          DATABASE,
          160015,
          "baiti_migrator",
          "baiti_migrator",
          MARKER,
          "baiti_migrator",
          false,
          "baiti_migrator",
          false,
          false,
          false),
      new NativePostgresIdentity(
          DATABASE,
          160015,
          "baiti_migrator",
          "baiti_migrator",
          MARKER,
          "baiti_migrator",
          false,
          "baiti_app",
          true,
          false,
          false),
      new NativePostgresIdentity(
          DATABASE,
          160015,
          "baiti_migrator",
          "baiti_migrator",
          MARKER,
          "baiti_migrator",
          false,
          "baiti_app",
          false,
          true,
          false),
      new NativePostgresIdentity(
          DATABASE,
          160015,
          "baiti_migrator",
          "baiti_migrator",
          MARKER,
          "baiti_migrator",
          false,
          "baiti_app",
          false,
          false,
          true)
    };
  }
}
