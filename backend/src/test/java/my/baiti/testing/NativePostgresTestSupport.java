package my.baiti.testing;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class NativePostgresTestSupport {

  @DynamicPropertySource
  static void registerNativeDatabase(DynamicPropertyRegistry registry) {
    var config = NativePostgresConfig.from(System.getenv());
    NativePostgresPreflight.verify(config);
    config.register(registry);
  }
}
