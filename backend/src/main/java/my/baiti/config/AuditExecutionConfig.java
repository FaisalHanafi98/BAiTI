package my.baiti.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
@Profile("!test | audit-test")
@EnableAsync
public class AuditExecutionConfig {
  @Bean(defaultCandidate = false)
  public ThreadPoolTaskExecutor tenantAuditExecutor() {
    var executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("tenant-audit-");
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(1);
    executor.setQueueCapacity(128);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(5);
    return executor;
  }
}
