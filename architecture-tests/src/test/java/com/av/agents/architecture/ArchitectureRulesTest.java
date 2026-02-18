package com.av.agents.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ArchitectureRulesTest {

  private static final JavaClasses CLASSES = new ClassFileImporter()
      .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
      .importPackages("com.av.agents");

  @Test
  void interfacesMustStartWithI() {
    classes().that().areInterfaces().should().haveSimpleNameStartingWith("I")
        .check(CLASSES);
  }

  @Test
  void repositoriesMustStartWithIAndBeInRepositoryPackage() {
    classes().that().haveSimpleNameEndingWith("Repository").should().haveSimpleNameStartingWith("I")
        .andShould().resideInAPackage("..repository..")
        .check(CLASSES);
  }

  @Test
  void entitiesMustEndWithEntityAndLiveInEntityOrDomainPackage() {
    classes().that().areAnnotatedWith("jakarta.persistence.Entity").should().haveSimpleNameEndingWith("Entity")
        .andShould().resideInAnyPackage("..entity..", "..domain..")
        .check(CLASSES);
  }

  @Test
  void sharedEntitiesOwnedBySharedPersistenceModule() {
    classes().that().haveSimpleName("TextArtifactEntity")
        .or().haveSimpleName("FinancialArtifactEntity")
        .or().haveSimpleName("ValidationArtifactEntity")
        .or().haveSimpleName("ReportArtifactEntity")
        .or().haveSimpleName("ApprovalRequestEntity")
        .or().haveSimpleName("EmailDeliveryEntity")
        .should().resideInAPackage("..sharedpersistence.entity..")
        .check(CLASSES);
  }
}
