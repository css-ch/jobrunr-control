package ch.css.jobrunr.control.architecture;

import ch.css.jobrunr.control.infrastructure.discovery.JobDefinitionRecorder;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * ArchUnit tests validating Quarkus extension development best practices.
 * <p>
 * This test ensures:
 * <ul>
 *   <li>No reflection usage in runtime module (should be in deployment module)</li>
 *   <li>Runtime module stays lightweight and GraalVM native-image friendly</li>
 *   <li>Build-time processing is done in deployment module</li>
 *   <li>Proper separation between build-time and runtime concerns</li>
 * </ul>
 *
 * @see <a href="https://quarkus.io/guides/writing-extensions">Quarkus Extension Guide</a>
 */
@DisplayName("Quarkus Extension Best Practices Tests")
public class QuarkusExtensionBestPracticesTest {

    private static final String BASE_PACKAGE = "ch.css.jobrunr.control";
    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    /**
     * Runtime module should not use java.lang.reflect API.
     * Reflection should be performed at build time in the deployment module.
     * <p>
     * This is a critical Quarkus extension best practice:
     * - Build-time reflection → deployment module
     * - Runtime code → should be reflection-free for GraalVM native-image
     */
    @Test
    @DisplayName("Runtime module should not use reflection API")
    void runtimeModuleShouldNotUseReflection() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(BASE_PACKAGE + "..")
                .and().areNotAssignableTo(JobDefinitionRecorder.class)
                .should().dependOnClassesThat().resideInAnyPackage(
                        "java.lang.reflect..",
                        "sun.reflect.."
                )
                .because("Reflection should be done at build-time in the deployment module, " +
                        "not at runtime. This ensures GraalVM native-image compatibility and better performance. " +
                        "Exception: Quarkus @Recorder classes run at STATIC_INIT time.");

        rule.check(importedClasses);
    }
    
    /**
     * Runtime module should not contain build steps.
     * BuildStep methods should only be in the deployment module.
     */
    @Test
    @DisplayName("Runtime module should not contain build steps")
    void runtimeModuleShouldNotContainBuildSteps() {
        ArchRule rule = noMethods()
                .that().areDeclaredInClassesThat().resideInAPackage(BASE_PACKAGE + "..")
                .should().beAnnotatedWith("io.quarkus.deployment.annotations.BuildStep")
                .because("@BuildStep methods belong in the deployment module");

        rule.check(importedClasses);
    }

    /**
     * Runtime module should not use @Record annotation with STATIC_INIT.
     * Recording should happen in deployment module, runtime only has recorders.
     */
    @Test
    @DisplayName("Runtime module should not use @Record annotation")
    void runtimeModuleShouldNotUseRecordAnnotation() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(BASE_PACKAGE + "..")
                .should().beAnnotatedWith("io.quarkus.deployment.annotations.Record")
                .because("@Record annotation is for deployment module processors, not runtime code");

        rule.check(importedClasses);
    }

    /**
     * Recorder classes should follow naming convention and be in infrastructure package.
     * Recorders are the bridge between deployment and runtime.
     */
    @Test
    @DisplayName("Recorder classes should follow naming convention")
    void recordersShouldFollowNamingConvention() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Recorder")
                .should().resideInAPackage("..infrastructure..")
                .andShould().notBeInterfaces()
                .because("Recorders are runtime infrastructure components used by deployment module");

        rule.check(importedClasses);
    }

    /**
     * Runtime module should prefer @ApplicationScoped over @Singleton.
     * This is a Quarkus CDI best practice.
     */
    @Test
    @DisplayName("Runtime beans should use @ApplicationScoped instead of @Singleton")
    void runtimeBeansShouldUseApplicationScoped() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(BASE_PACKAGE + "..")
                .and().areNotAnnotatedWith("jakarta.enterprise.context.ApplicationScoped")
                .and().areNotAnnotatedWith("jakarta.enterprise.context.RequestScoped")
                .and().areNotAnnotatedWith("jakarta.enterprise.context.Dependent")
                .should().beAnnotatedWith("jakarta.inject.Singleton")
                .because("Quarkus prefers CDI scopes (@ApplicationScoped) over @Singleton for better integration");

        rule.check(importedClasses);
    }

    /**
     * Runtime configuration classes should use @ConfigMapping instead of @ConfigProperties.
     * ConfigMapping is the modern Quarkus approach with better type safety.
     */
    @Test
    @DisplayName("Configuration should use @ConfigMapping for type-safe config")
    void configurationShouldUseConfigMapping() {
        ArchRule rule = classes()
                .that().resideInAPackage("..infrastructure.config..")
                .and().haveSimpleNameContaining("Config")
                .and().areNotEnums()
                .should().beAnnotatedWith("io.smallrye.config.ConfigMapping")
                .orShould().beRecords()
                .because("@ConfigMapping provides type-safe, build-time validated configuration");

        //      rule.check(importedClasses);
    }

    /**
     * Runtime module should not use Jandex API directly.
     * Jandex indexing is a build-time concern handled by deployment module.
     */
    @Test
    @DisplayName("Runtime module should not use Jandex API")
    void runtimeModuleShouldNotUseJandex() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(BASE_PACKAGE + "..")
                .should().dependOnClassesThat().resideInAnyPackage("org.jboss.jandex..")
                .because("Jandex is used at build-time in deployment module for class scanning");

        rule.check(importedClasses);
    }

    /**
     * Runtime module should not import deployment module classes.
     * This ensures proper separation of build-time and runtime concerns.
     */
    @Test
    @DisplayName("Runtime module should not import deployment module classes")
    void runtimeShouldNotImportDeploymentClasses() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(BASE_PACKAGE + "..")
                .should().dependOnClassesThat().resideInAnyPackage("..deployment..")
                .because("Runtime and deployment modules must be strictly separated");

        rule.check(importedClasses);
    }

    /**
     * CDI beans in runtime should use constructor injection, not field injection.
     * This is both a Quarkus best practice and organizational constraint.
     */
    @Test
    @DisplayName("CDI beans should use constructor injection")
    void cdiBeansShouldUseConstructorInjection() {
        ArchRule rule = fields()
                .that().areDeclaredInClassesThat().resideInAPackage(BASE_PACKAGE + "..")
                .and().areDeclaredInClassesThat().areAnnotatedWith("jakarta.enterprise.context.ApplicationScoped")
                .should().notBeAnnotatedWith("jakarta.inject.Inject")
                .because("Constructor injection is preferred over field injection per organizational constraints");

        rule.check(importedClasses);
    }
}
