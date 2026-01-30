package ch.css.jobrunr.control.ui;

import com.microsoft.playwright.Locator;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI test for starting a template job directly from the Templates UI using the "Start" button.
 * Verifies:
 * - Template can be started via UI button
 * - Execution appears in history
 * - Template remains available for reuse
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TemplateStartUITest extends JobTriggerUITestBase {

    private static UUID templateId;
    private static final String TEMPLATE_NAME = "Start Button Test Template";

    @Test
    @Order(1)
    @DisplayName("Create a template job for UI start test")
    public void testCreateTemplate() {
        navigateToTemplatesPage();
        openTemplateCreationDialog();
        selectTemplateJobType("ParameterDemoJob");
        fillTemplateName(TEMPLATE_NAME);
        fillParameterDemoJobParameters("Test String", "99", "false", "OPTION_A");
        submitTemplateCreationForm();

        templateId = extractTemplateIdFromTemplatesTable(TEMPLATE_NAME);
        System.out.println("Created template ID: " + templateId);
        assertNotNull(templateId, "Template ID should be extracted successfully");
    }

    @Test
    @Order(2)
    @DisplayName("Start template using UI Start button")
    public void testStartTemplateViaUI() {
        assertNotNull(templateId, "Template ID should be set from previous test");

        navigateToTemplatesPage();

        // Find the template row
        Locator templateRow = page.locator("tr:has-text('" + TEMPLATE_NAME + "')");
        assertTrue(templateRow.isVisible(), "Template should be visible in templates table");

        // Click the Start button (green button with play icon)
        Locator startButton = templateRow.locator("button.btn-outline-success[title='Jetzt starten']");
        assertTrue(startButton.isVisible(), "Start button should be visible");

        // Handle the confirmation dialog
        page.onDialog(dialog -> {
            System.out.println("Confirmation dialog: " + dialog.message());
            assertTrue(dialog.message().contains(TEMPLATE_NAME),
                    "Confirmation should mention template name");
            dialog.accept();
        });

        startButton.click();

        // Wait for the table to refresh
        page.waitForTimeout(1000);

        // Template should still be visible in the table
        assertTrue(templateRow.isVisible(), "Template should remain visible after starting");
    }

    @Test
    @Order(3)
    @DisplayName("Verify started job appears in history")
    public void testStartedJobInHistory() {
        assertNotNull(templateId, "Template ID should be set from previous test");

        navigateToHistory();

        // Search for jobs with the template name (the cloned job will have a timestamp appended)
        searchForJob(TEMPLATE_NAME);

        // Wait for at least one job to appear
        Locator jobRows = page.locator("tr:has-text('" + TEMPLATE_NAME + "')");
        jobRows.first().waitFor(new Locator.WaitForOptions().setTimeout(5000));

        int jobCount = jobRows.count();
        System.out.println("Found " + jobCount + " job(s) in history with template name");
        assertTrue(jobCount >= 1, "At least one executed job should appear in history");

        // Verify the job has a valid status
        String firstJobRowText = jobRows.first().innerText();
        System.out.println("First executed job in history: " + firstJobRowText);
        assertTrue(
                firstJobRowText.contains("SUCCEEDED") ||
                        firstJobRowText.contains("PROCESSING") ||
                        firstJobRowText.contains("ENQUEUED"),
                "Executed job should have a valid execution status"
        );
    }

    @Test
    @Order(4)
    @DisplayName("Verify template can be started multiple times via UI")
    public void testStartTemplateMultipleTimes() {
        assertNotNull(templateId, "Template ID should be set from previous test");

        navigateToTemplatesPage();

        // Start the template again
        Locator templateRow = page.locator("tr:has-text('" + TEMPLATE_NAME + "')");
        Locator startButton = templateRow.locator("button.btn-outline-success[title='Jetzt starten']");

        assertTrue(startButton.isVisible(), "Start button should still be visible");

        page.onDialog(dialog -> dialog.accept());
        startButton.click();
        page.waitForTimeout(1000);

        // Verify multiple executions in history
        navigateToHistory();
        searchForJob(TEMPLATE_NAME);

        Locator jobRows = page.locator("tr:has-text('" + TEMPLATE_NAME + "')");
        jobRows.first().waitFor(new Locator.WaitForOptions().setTimeout(5000));

        int jobCount = jobRows.count();
        System.out.println("Found " + jobCount + " job(s) after second start");
        assertTrue(jobCount >= 2, "At least two executed jobs should appear in history after starting twice");
    }

    @Test
    @Order(5)
    @DisplayName("Verify template remains unchanged after multiple starts")
    public void testTemplateUnchangedAfterStarts() {
        assertNotNull(templateId, "Template ID should be set from previous test");

        navigateToTemplatesPage();

        Locator templateRow = page.locator("tr:has-text('" + TEMPLATE_NAME + "')");
        assertTrue(templateRow.isVisible(), "Template should still be visible");

        String rowText = templateRow.innerText();
        System.out.println("Template row after multiple starts: " + rowText);

        // Verify template name is exactly as created
        Locator templateLink = page.locator("strong a:has-text('" + TEMPLATE_NAME + "')");
        assertTrue(templateLink.isVisible(), "Template should have its original name");

        // Verify it's still marked as a template
        assertTrue(rowText.contains("Template"), "Row should still be marked as Template");
        assertTrue(rowText.contains("ParameterDemoJob"), "Template should still show correct job type");
    }
}
