package my.baiti.testing;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import my.baiti.system.ActivityLog;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.ValueBinder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ActivityLogAddressIT {

  @ParameterizedTest
  @ValueSource(strings = {"192.0.2.17", "2001:db8::17"})
  void entityAddressMappingBindsAndReadsNativeInet(String address) throws Exception {
    var config = NativePostgresConfig.from(System.getenv());
    NativePostgresPreflight.verify(config);
    var registry =
        new StandardServiceRegistryBuilder()
            .applySetting("hibernate.dialect", PostgreSQLDialect.class.getName())
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
            .build();
    try {
      var metadata =
          new MetadataSources(registry).addAnnotatedClass(ActivityLog.class).buildMetadata();
      var addressType =
          (BasicType<?>)
              metadata
                  .getEntityBinding(ActivityLog.class.getName())
                  .getProperty("ipAddress")
                  .getType();
      Object value = addressType.getJavaTypeDescriptor().fromString(address);
      try (var connection = config.openAppConnection();
          var statement = connection.prepareStatement("SELECT host(?)")) {
        bind(addressType, statement, value);
        try (var result = statement.executeQuery()) {
          assertThat(result.next()).isTrue();
          assertThat(InetAddress.getByName(result.getString(1)))
              .isEqualTo(InetAddress.getByName(address));
        }
      }
    } finally {
      StandardServiceRegistryBuilder.destroy(registry);
    }
  }

  @SuppressWarnings("unchecked")
  private static void bind(BasicType<?> type, java.sql.PreparedStatement statement, Object value)
      throws Exception {
    ((ValueBinder<Object>) type.getJdbcValueBinder()).bind(statement, value, 1, null);
  }
}
