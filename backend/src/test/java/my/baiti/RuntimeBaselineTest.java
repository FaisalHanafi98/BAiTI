package my.baiti;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;

class RuntimeBaselineTest {

  @Test
  void applicationLoadsTheSelectedSupportedSpringLine() {
    assertThat(SpringBootVersion.getVersion()).startsWith("4.1.");
  }
}
