package ch.css.jobrunr.control.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit tests validating logging conventions.
 * <p>
 * These tests ensure:
 * <ul>
 *   <li>Only JBoss Logging is used (org.jboss.logging.Logger)</li>
 *   <li>No SLF4J, Log4j, java.util.logging, or other logging frameworks</li>
 *   <li>Logger constant is uppercase LOG</li>
 *   <li>Logger constant is static final</li>
 * </ul>
 *
 * @see <a href="../../../../../../.github/copilot-instructions.md">Copilot Instructions - Logging</a>
 */
@DisplayName("Logging Conventions Tests")
class LoggingConventionsTest {

    private static final String BASE_PACKAGE = "ch.css.jobrunr.control";
    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    // ========================================
    // JBoss Logging Enforcement
    // ========================================

    @Test
    @DisplayName("Should only use JBoss Logging (org.jboss.logging.Logger)")
    void shouldOnlyUseJBossLogging() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().haveFullyQualifiedName("org.slf4j.Logger")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("org.slf4j.LoggerFactory")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("org.apache.logging.log4j.Logger")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("org.apache.logging.log4j.LogManager")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("java.util.logging.Logger")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("org.apache.commons.logging.Log")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("org.apache.commons.logging.LogFactory")
                .because("Only JBoss Logging (org.jboss.logging.Logger) is allowed as per project conventions");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Logger fields should be named 'LOG' (uppercase)")
    void loggerFieldsShouldBeUppercase() {
        ArchRule rule = fields()
                .that().haveRawType("org.jboss.logging.Logger")
                .should().haveName("LOG")
                .because("Logger constant must be uppercase per Java naming conventions");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Logger fields should be static final")
    void loggerFieldsShouldBeStaticFinal() {
        ArchRule rule = fields()
                .that().haveRawType("org.jboss.logging.Logger")
                .should().beStatic()
                .andShould().beFinal()
                .because("Logger should be a static final constant");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Logger fields should be private")
    void loggerFieldsShouldBePrivate() {
        ArchRule rule = fields()
                .that().haveRawType("org.jboss.logging.Logger")
                .should().bePrivate()
                .because("Logger fields should be encapsulated");

        rule.check(importedClasses);
    }

    // ========================================
    // Negative Tests - What Should NOT Be Used
    // ========================================

    @Test
    @DisplayName("Should not import SLF4J Logger")
    void shouldNotImportSlf4jLogger() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().haveFullyQualifiedName("org.slf4j.Logger")
                .because("SLF4J is not allowed - use org.jboss.logging.Logger instead");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Should not import Log4j Logger")
    void shouldNotImportLog4jLogger() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().haveFullyQualifiedName("org.apache.logging.log4j.Logger")
                .because("Log4j is not allowed - use org.jboss.logging.Logger instead");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Should not import java.util.logging.Logger")
    void shouldNotImportJavaUtilLogging() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().haveFullyQualifiedName("java.util.logging.Logger")
                .because("java.util.logging is not allowed - use org.jboss.logging.Logger instead");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Should not use System.out.println or System.err.println")
    void shouldNotUseSystemOutOrErr() {
        ArchRule rule = noClasses()
                .should().accessField("java.lang.System", "out")
                .orShould().accessField("java.lang.System", "err")
                .because("Use proper logging instead of System.out/err");

        rule.check(importedClasses);
    }
}
