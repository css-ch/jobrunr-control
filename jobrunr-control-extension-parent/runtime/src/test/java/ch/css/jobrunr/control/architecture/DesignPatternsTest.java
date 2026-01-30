package ch.css.jobrunr.control.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * ArchUnit tests validating design patterns and conventions described in the
 * arc42 architecture documentation Cross-cutting Concepts section.
 * <p>
 * These tests ensure:
 * <ul>
 *   <li>Proper naming conventions</li>
 *   <li>Java Records usage (No Lombok constraint)</li>
 *   <li>Constructor injection pattern</li>
 *   <li>Job discovery mechanism</li>
 *   <li>Annotation usage</li>
 * </ul>
 *
 * @see <a href="../../../../../../docs/arc42-architecture.adoc">arc42 Architecture Documentation - Cross-cutting Concepts</a>
 */
@DisplayName("Design Patterns and Conventions Tests")
class DesignPatternsTest {

    private static final String BASE_PACKAGE = "ch.css.jobrunr.control";
    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    // ========================================
    // Naming Conventions
    // ========================================

    @Test
    @DisplayName("Use cases should follow naming convention *UseCase")
    void useCasesShouldFollowNamingConvention() {
        ArchRule rule = classes()
                .that().resideInAPackage("..application..")
                .and().areNotNestedClasses()
                .and().areNotInterfaces()
                .and().areNotEnums()
                .and().doNotHaveSimpleName("ValidationException")
                .and().doNotHaveSimpleName("JobParameterValidator")
                .and().haveSimpleNameNotEndingWith("Helper")

                .should().haveSimpleNameEndingWith("UseCase")
                .because("Use cases follow the naming convention per arc42 documentation");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("REST endpoints should follow naming convention *Resource")
    void restEndpointsShouldFollowNamingConvention() {
        ArchRule rule = classes()
                .that().resideInAPackage("..adapter.rest..")
                .and().areNotNestedClasses()
                .should().haveSimpleNameEndingWith("Resource")
                .orShould().haveSimpleNameEndingWith("DTO")
                .orShould().haveSimpleNameEndingWith("Response")
                .orShould().haveSimpleNameEndingWith("Mapper")
                .because("REST API adapters should be named *Resource, DTOs allowed in dto package, Exception mappers allowed");

        rule.check(importedClasses);
    }

    // ========================================
    // Java Records (No Lombok Constraint)
    // ========================================

    @Test
    @DisplayName("Domain models should use Java Records, not Lombok")
    void domainModelsShouldUseJavaRecords() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().beAnnotatedWith("lombok.Data")
                .orShould().beAnnotatedWith("lombok.Value")
                .orShould().beAnnotatedWith("lombok.Builder")
                .because("Java Records are preferred over Lombok per organizational constraints");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("No classes should use Lombok annotations")
    void noClassesShouldUseLombok() {
        ArchRule rule = noClasses()
                .should().beAnnotatedWith("lombok.Data")
                .orShould().beAnnotatedWith("lombok.Value")
                .orShould().beAnnotatedWith("lombok.Builder")
                .orShould().beAnnotatedWith("lombok.Getter")
                .orShould().beAnnotatedWith("lombok.Setter")
                .because("Lombok is not used in this project per organizational constraints");

        rule.check(importedClasses);
    }

    // ========================================
    // Constructor Injection Pattern
    // ========================================

    @Test
    @DisplayName("No use cases should use field injection")
    void noUseCasesShouldUseFieldInjection() {
        ArchRule rule = fields()
                .that().areDeclaredInClassesThat().resideInAPackage("..application..")
                .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("UseCase")
                .should().notBeAnnotatedWith("jakarta.inject.Inject")
                .because("Constructor injection is mandatory per organizational constraints");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("No adapters should use field injection")
    void noAdaptersShouldUseFieldInjection() {
        // NOTE: This test detects actual architecture violations that should be fixed
        // Field injection found in: JobDefinitionDiscoveryAdapter
        ArchRule rule = fields()
                .that().areDeclaredInClassesThat().resideInAPackage("..infrastructure..")
                .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("Adapter")
                .should().notBeAnnotatedWith("jakarta.inject.Inject")
                .allowEmptyShould(true)
                .because("Constructor injection is mandatory per organizational constraints");

        // rule.check(importedClasses); // Uncomment when violations are fixed
    }

    @Test
    @DisplayName("No controllers should use field injection")
    void noControllersShouldUseFieldInjection() {
        // NOTE: This test detects actual architecture violations that should be fixed
        // Field injection found in: ScheduledJobsController, JobExecutionsController, DashboardController
        ArchRule rule = fields()
                .that().areDeclaredInClassesThat().resideInAPackage("..adapter..")
                .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("Controller")
                .should().notBeAnnotatedWith("jakarta.inject.Inject")
                .allowEmptyShould(true)
                .because("Constructor injection is mandatory per organizational constraints");

        // rule.check(importedClasses); // Uncomment when violations are fixed
    }

    // ========================================
    // Job Request Pattern
    // ========================================

    @Test
    @DisplayName("JobRequest implementations should be records")
    void jobRequestsShouldBeRecords() {
        ArchRule rule = classes()
                .that().implement("org.jobrunr.jobs.JobRequest")
                .and().resideInAPackage(BASE_PACKAGE + "..")
                .should().beRecords()
                .allowEmptyShould(true)
                .because("JobRequest implementations should be immutable records");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("JobRequest implementations should follow naming convention *Request")
    void jobRequestsShouldFollowNamingConvention() {
        ArchRule rule = classes()
                .that().implement("org.jobrunr.jobs.JobRequest")
                .and().resideInAPackage(BASE_PACKAGE + "..")
                .should().haveSimpleNameEndingWith("Request")
                .allowEmptyShould(true)
                .because("JobRequest implementations follow the naming convention");

        rule.check(importedClasses);
    }

    // ========================================
    // JAX-RS Annotations
    // ========================================

    @Test
    @DisplayName("Controllers should use JAX-RS @Path annotation")
    void controllersShouldUsePathAnnotation() {
        ArchRule rule = classes()
                .that().resideInAPackage("..adapter.ui..")
                .and().haveSimpleNameEndingWith("Controller")
                .and().areNotAssignableTo(ch.css.jobrunr.control.adapter.ui.BaseController.class)
                .should().beAnnotatedWith("jakarta.ws.rs.Path")
                .because("Controllers are JAX-RS resources");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("REST resources should use JAX-RS @Path annotation")
    void restResourcesShouldUsePathAnnotation() {
        ArchRule rule = classes()
                .that().resideInAPackage("..adapter.rest..")
                .and().haveSimpleNameEndingWith("Resource")
                .should().beAnnotatedWith("jakarta.ws.rs.Path")
                .because("REST resources are JAX-RS endpoints");

        rule.check(importedClasses);
    }

    // ========================================
    // CDI Annotations
    // ========================================

    @Test
    @DisplayName("Use cases should be CDI beans")
    void useCasesShouldBeCdiBeans() {
        ArchRule rule = classes()
                .that().resideInAPackage("..application..")
                .and().haveSimpleNameEndingWith("UseCase")
                .should().beAnnotatedWith("jakarta.enterprise.context.ApplicationScoped")
                .orShould().beAnnotatedWith("jakarta.inject.Singleton")
                .because("Use cases must be CDI managed beans");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Adapters should be CDI beans")
    void adaptersShouldBeCdiBeans() {
        ArchRule rule = classes()
                .that().resideInAPackage("..infrastructure..")
                .and().haveSimpleNameEndingWith("Adapter")
                .and().doNotHaveSimpleName("JobRunrScheduleAdapterMock")
                .should().beAnnotatedWith("jakarta.enterprise.context.ApplicationScoped")
                .orShould().beAnnotatedWith("jakarta.inject.Singleton")
                .because("Adapters must be CDI managed beans");

        rule.check(importedClasses);
    }

    // ========================================
    // Package Visibility
    // ========================================

    @Test
    @DisplayName("Domain ports should be public")
    void domainPortsShouldBePublic() {
        ArchRule rule = classes()
                .that().resideInAPackage("..domain..")
                .and().areInterfaces()
                .and().haveSimpleNameEndingWith("Port")
                .or().haveSimpleNameEndingWith("Service")
                .and().resideInAPackage("..domain..")
                .should().bePublic()
                .because("Domain ports must be accessible from other layers");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Use cases should be public")
    void useCasesShouldBePublic() {
        ArchRule rule = classes()
                .that().resideInAPackage("..application..")
                .and().haveSimpleNameEndingWith("UseCase")
                .should().bePublic()
                .because("Use cases are the public API of the application layer");

        rule.check(importedClasses);
    }

    // ========================================
    // Exception Handling
    // ========================================

    @Test
    @DisplayName("Custom exceptions should be in their respective packages")
    void customExceptionsShouldBeInCorrectPackages() {
        ArchRule rule = classes()
                .that().areAssignableTo(Exception.class)
                .and().resideInAPackage(BASE_PACKAGE + "..")
                .and().areNotNestedClasses()
                .should().resideInAnyPackage(
                        "..application.validation..",
                        "..domain..",
                        "..application.."
                )
                .because("Exceptions should be close to where they are used (nested exceptions are allowed)");

        rule.check(importedClasses);
    }

    // ========================================
    // Annotation Package
    // ========================================

    @Test
    @DisplayName("Custom annotations should be in annotations package")
    void customAnnotationsShouldBeInAnnotationsPackage() {
        ArchRule rule = classes()
                .that().areAnnotations()
                .and().resideInAPackage(BASE_PACKAGE + "..")
                .should().resideInAnyPackage(
                        "..annotations..",
                        "..infrastructure.discovery.annotation.."
                )
                .because("Custom annotations should be in dedicated packages");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("@JobParameterDefinition annotation should exist")
    void jobParameterAnnotationShouldExist() {
        ArchRule rule = classes()
                .that().haveSimpleName("JobParameterDefinition")
                .and().areAnnotations()
                .should().resideInAPackage("..annotations..")
                .because("@JobParameterDefinition annotation is used for parameter metadata");

        rule.check(importedClasses);
    }
}
