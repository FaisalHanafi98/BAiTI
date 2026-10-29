package my.baiti.arch;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import my.baiti.common.TenantScopedBaseEntity;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

class ArchitectureTest {

  private static final JavaClasses MAIN =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("my.baiti");

  @Test
  void tenantEntitiesInheritTenantAndAuditFields() {
    classes()
        .that()
        .areAnnotatedWith(Entity.class)
        .and()
        .doNotHaveSimpleName("Tenant")
        .and()
        .doNotHaveSimpleName("SuperAdmin")
        .should()
        .beAssignableTo(TenantScopedBaseEntity.class)
        .check(MAIN);
  }

  @Test
  void controllersCannotDependOnRepositoriesOrEntities() {
    noClasses()
        .that()
        .haveSimpleNameEndingWith("Controller")
        .should()
        .dependOnClassesThat()
        .areAssignableTo(JpaRepository.class)
        .allowEmptyShould(true)
        .check(MAIN);
    noClasses()
        .that()
        .haveSimpleNameEndingWith("Controller")
        .should()
        .dependOnClassesThat()
        .areAnnotatedWith(Entity.class)
        .allowEmptyShould(true)
        .check(MAIN);
  }

  @Test
  void foreignModulesCannotAccessDomainRepositories() {
    for (String feature :
        new String[] {
          "tenancy",
          "security",
          "people",
          "property",
          "finance",
          "complaints",
          "maintenance",
          "facilities",
          "community",
          "visitors",
          "system"
        }) {
      noClasses()
          .that()
          .resideOutsideOfPackage("my.baiti." + feature + "..")
          .should()
          .dependOnClassesThat(
              resideInAPackage("my.baiti." + feature + "..").and(assignableTo(JpaRepository.class)))
          .check(MAIN);
    }
  }

  @Test
  void jwtImplementationLibrariesStayInsideSecurity() {
    noClasses()
        .that()
        .resideOutsideOfPackage("my.baiti.security..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("io.jsonwebtoken..", "com.auth0.jwt..")
        .check(MAIN);
  }

  @Test
  void entityTimestampsDoNotUseLocalDateTime() {
    noFields()
        .that()
        .areDeclaredInClassesThat()
        .areAnnotatedWith(Entity.class)
        .or()
        .areDeclaredInClassesThat()
        .areAnnotatedWith(MappedSuperclass.class)
        .should()
        .haveRawType(LocalDateTime.class)
        .check(MAIN);
  }
}
