package my.baiti.testing;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class NativeBootstrapArchitectureTest {

  @Test
  void nativeIntegrationBootstrapCannotInitializeContainerClasses() {
    noClasses()
        .that()
        .haveSimpleName("HealthIT")
        .or()
        .haveNameMatching(".*NativePostgres.*")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("org.testcontainers..")
        .check(new ClassFileImporter().importPackages("my.baiti"));
  }
}
