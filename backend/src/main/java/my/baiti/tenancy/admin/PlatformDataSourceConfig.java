package my.baiti.tenancy.admin;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
@UsesPrivilegedDataSource
public class PlatformDataSourceConfig {

  @Bean(destroyMethod = "close")
  public HikariDataSource platformConnectionPool(
      @Value("${spring.flyway.url}") String url,
      @Value("${spring.flyway.user}") String user,
      @Value("${spring.flyway.password}") String password) {
    var pool = new HikariDataSource();
    pool.setJdbcUrl(url);
    pool.setUsername(user);
    pool.setPassword(password);
    pool.setPoolName("baiti-platform-pool");
    pool.setMaximumPoolSize(2);
    pool.setMinimumIdle(0);
    pool.setConnectionTimeout(5000);
    pool.addDataSourceProperty("connectTimeout", "5");
    pool.addDataSourceProperty("socketTimeout", "10");
    return pool;
  }

  @Bean
  public JdbcTemplate platformJdbcTemplate(@Qualifier("platformConnectionPool") DataSource source) {
    return new JdbcTemplate(source);
  }

  @Bean
  @Primary
  public JdbcTemplate appJdbcTemplate(@Qualifier("appDataSource") DataSource source) {
    return new JdbcTemplate(source);
  }
}
