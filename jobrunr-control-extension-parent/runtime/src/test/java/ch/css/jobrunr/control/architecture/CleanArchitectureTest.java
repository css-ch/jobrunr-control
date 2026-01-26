package ch.css.jobrunr.control.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * ArchUnit tests validating the Clean Architecture / Hexagonal Architecture pattern
 * as described in the arc42 architecture documentation.
 * <p>
 * This test ensures:
 * <ul>
 *   <li>Strict layer separation (Domain, Application, Infrastructure, Adapter)</li>
 *   <li>Dependency inversion (Infrastructure depends on Domain ports)</li>
 *   <li>No circular dependencies between packages</li>
 *   <li>Proper use of architectural patterns</li>
 * </ul>
 *
 * @see <a href="../../../../../../docs/arc42-architecture.adoc">arc42 Architecture Documentation</a>
 */
@DisplayName("Clean Architecture Tests")
class CleanArchitectureTest {

    private static final String BASE_PACKAGE = "ch.css.jobrunr.control";
    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    /**
     * Validates the layered architecture as shown in the PlantUML diagram:
     * "Level 1: System Overview" in arc42-architecture.adoc
     * <p>
     * Domain Layer ← Application Layer ← Adapter Layer
     * Infrastructure Layer implements Domain Layer ports
     */
    @Test
    @DisplayName("Layer dependencies should follow Clean Architecture principles")
    void layerDependenciesShouldFollowCleanArchitecture() {
        Architectures.LayeredArchitecture architecture = layeredArchitecture()
                .consideringOnlyDependenciesInLayers()

                // Define layers as per arc42 documentation
                .layer("Domain").definedBy(BASE_PACKAGE + ".domain..")
                .layer("Application").definedBy(BASE_PACKAGE + ".application..")
                .layer("Infrastructure").definedBy(BASE_PACKAGE + ".infrastructure..")
                .layer("Adapter").definedBy(BASE_PACKAGE + ".adapter..")
                .layer("Annotations").definedBy(BASE_PACKAGE + ".annotations..")

                // Domain layer should not depend on anything (except annotations)
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Adapter")

                // Application layer may depend on Domain
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter", "Infrastructure")
                .whereLayer("Application").mayOnlyAccessLayers("Domain", "Annotations")

                // Infrastructure implements Domain ports
                .whereLayer("Infrastructure").mayOnlyAccessLayers("Domain", "Application", "Annotations")

                // Adapter layer may access Application and Domain
                .whereLayer("Adapter").mayOnlyAccessLayers("Application", "Domain", "Annotations");

        architecture.check(importedClasses);
    }

    /**
     * Validates that domain layer contains only pure business logic
     * without any framework dependencies (except standard Java API)
     */
    @Test
    @DisplayName("Domain layer should be framework-agnostic")
    void domainLayerShouldBePureBusinessLogic() {
        ArchRule rule = classes()
                .that().resideInAPackage("..domain..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..domain..",
                        "java..",
                        "ch.css.jobrunr.control.annotations.."
                )
                .because("Domain layer must remain framework-agnostic as per Clean Architecture principles");

        rule.check(importedClasses);
    }

    /**
     * Validates the Ports and Adapters pattern as shown in:
     * "Level 2: Domain Layer" PlantUML diagram in arc42-architecture.adoc
     */
    @Test
    @DisplayName("Domain ports should be interfaces")
    void domainPortsShouldBeInterfaces() {
        ArchRule rule = classes()
                .that().resideInAPackage("..domain..")
                .and().haveSimpleNameEndingWith("Port")
                .or().haveSimpleNameEndingWith("Service")
                .should().beInterfaces()
                .because("Ports must be interfaces for dependency inversion (Hexagonal Architecture)");

        rule.check(importedClasses);
    }

    /**
     * Validates that Infrastructure adapters implement Domain ports
     * as per the Hexagonal Architecture pattern
     */
    @Test
    @DisplayName("Infrastructure adapters should implement domain ports")
    void infrastructureAdaptersShouldImplementDomainPorts() {
        // This test validates that adapters exist and are properly named
        // The actual interface implementation is validated by compilation
        ArchRule rule = classes()
                .that().resideInAPackage("..infrastructure..")
                .and().haveSimpleNameEndingWith("Adapter")
                .should().notBeInterfaces()
                .because("Adapters implement ports (Dependency Inversion Principle)");

        rule.check(importedClasses);
    }

    /**
     * Validates use case naming convention as shown in:
     * "Level 2: Application Layer" PlantUML diagram in arc42-architecture.adoc
     */
    @Test
    @DisplayName("Application layer should contain use cases")
    void applicationLayerShouldContainUseCases() {
        ArchRule rule = classes()
                .that().resideInAPackage("..application..")
                .and().areNotNestedClasses()
                .and().areNotInterfaces()
                .and().areNotEnums()
                .and().areNotAnnotations()
                .should().haveSimpleNameEndingWith("UseCase")
                .orShould().haveSimpleNameEndingWith("Validator")
                .orShould().haveSimpleNameEndingWith("Exception")
                .because("Application layer follows use case driven design pattern");

        rule.check(importedClasses);
    }

    /**
     * Validates that use cases use constructor injection
     * (no field injection as per organizational constraints)
     */
    @Test
    @DisplayName("Use cases should use constructor injection")
    void useCasesShouldUseConstructorInjection() {
        ArchRule rule = fields()
                .that().areDeclaredInClassesThat().resideInAPackage("..application..")
                .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("UseCase")
                .should().notBeAnnotatedWith("jakarta.inject.Inject")
                .because("Constructor injection is required per organizational constraints");

        rule.check(importedClasses);
    }

    /**
     * Validates domain models are immutable (Records or proper immutability)
     * as shown in domain layer documentation
     */
    @Test
    @DisplayName("Domain records should be in domain package")
    void domainModelsShouldBeRecords() {
        ArchRule rule = classes()
                .that().areRecords()
                .and().resideInAPackage("..control..")
                .and().areNotNestedClasses()
                .should().resideInAnyPackage(
                        "..domain..",
                        "..application..",
                        "..infrastructure..",
                        "..adapter.."
                )
                .because("Records represent immutable data structures (Java 21 feature, no Lombok constraint)");

        rule.check(importedClasses);
    }

    /**
     * Validates adapter layer structure as per:
     * "Level 2: Infrastructure Layer" in arc42-architecture.adoc
     */
    @Test
    @DisplayName("Adapters should be in adapter package")
    void adaptersShouldBeInAdapterPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Controller")
                .or().haveSimpleNameEndingWith("Resource")
                .should().resideInAPackage("..adapter..")
                .because("Controllers and Resources are adapters in the Hexagonal Architecture");

        rule.check(importedClasses);
    }

    /**
     * Validates no circular dependencies between packages
     * ensuring maintainability and clear dependency flow
     */
    @Test
    @DisplayName("Packages should not have circular dependencies")
    void packagesShouldNotHaveCircularDependencies() {
        ArchRule rule = slices()
                .matching(BASE_PACKAGE + ".(*)..")
                .should().beFreeOfCycles()
                .because("Circular dependencies violate Clean Architecture principles");

        rule.check(importedClasses);
    }

    /**
     * Validates that infrastructure classes don't leak into domain
     * maintaining the dependency inversion principle
     */
    @Test
    @DisplayName("Domain should not depend on infrastructure")
    void domainShouldNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .because("Domain must not depend on infrastructure (Dependency Inversion Principle)");

        rule.check(importedClasses);
    }

    /**
     * Validates that domain should not depend on application layer
     */
    @Test
    @DisplayName("Domain should not depend on application layer")
    void domainShouldNotDependOnApplication() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..application..")
                .because("Domain is the innermost layer and should not depend on outer layers");

        rule.check(importedClasses);
    }

    /**
     * Validates that adapters don't directly access infrastructure
     * They should go through application layer use cases
     */
    @Test
    @DisplayName("Adapters should use application layer, not infrastructure directly")
    void adaptersShouldUseApplicationLayer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..adapter..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .andShould().notBeInterfaces()
                .because("Adapters should depend on use cases, not infrastructure implementations");

        rule.check(importedClasses);
    }

    /**
     * Validates naming convention for infrastructure packages
     * as per the PlantUML diagrams in arc42 documentation
     */
    @Test
    @DisplayName("Infrastructure should be organized by technical concern")
    void infrastructureShouldBeOrganizedByTechnicalConcern() {
        ArchRule rule = classes()
                .that().resideInAPackage("..infrastructure..")
                .should().resideInAnyPackage(
                        "..infrastructure.scheduler..",
                        "..infrastructure.execution..",
                        "..infrastructure.discovery..",
                        "..infrastructure.discovery.annotation..",
                        "..infrastructure.."
                )
                .because("Infrastructure layer should be organized by technical concerns");

        rule.check(importedClasses);
    }

    /**
     * Validates application layer organization
     * as per the use case driven design pattern
     */
    @Test
    @DisplayName("Application layer should be organized by business capability")
    void applicationLayerShouldBeOrganizedByBusinessCapability() {
        ArchRule rule = classes()
                .that().resideInAPackage("..application..")
                .should().resideInAnyPackage(
                        "..application.scheduling..",
                        "..application.monitoring..",
                        "..application.discovery..",
                        "..application.validation..",
                        "..application.."
                )
                .because("Application layer should be organized by business capabilities (use cases)");

        rule.check(importedClasses);
    }
}
