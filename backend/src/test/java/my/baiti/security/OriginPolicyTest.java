package my.baiti.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OriginPolicyTest {

  @Test
  void explicitOriginsAreRetainedWithoutWildcardExpansion() {
    assertThat(
            OriginPolicy.parse("https://synthetic.example.invalid,http://localhost:5173").origins())
        .containsExactlyInAnyOrder("https://synthetic.example.invalid", "http://localhost:5173");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "*",
        "https://*.example.invalid",
        "https://synthetic.example.invalid/path",
        "https://synthetic-user@example.invalid",
        "https://synthetic.example.invalid?query=synthetic",
        "https://synthetic.example.invalid#fragment",
        "https://synthetic.example.invalid:99999"
      })
  void malformedOrBroadOriginsFailAtStartup(String value) {
    assertThatIllegalArgumentException().isThrownBy(() -> OriginPolicy.parse(value));
  }
}
