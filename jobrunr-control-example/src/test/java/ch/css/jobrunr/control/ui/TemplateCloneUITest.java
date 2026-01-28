package ch.css.jobrunr.control.ui;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI test for cloning a template job and verifying:
 * - Template is created successfully
 * - Template can be cloned via UI
 * - Both original and cloned templates exist
 * - Cloned template has same parameters as original
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TemplateCloneUITest extends JobTriggerUITestBase {

    private static UUID originalTemplateId;
    private static UUID clonedTemplateId;
    private static final String ORIGINAL_TEMPLATE_NAME = "Test Template - Clone Original";
    private static String expectedClonedTemplateName;

    @Test
    @Order(1)
    @DisplayName("Create a template job via Templates UI")
    public void testCreateTemplate() {
        navigateToTemplatesPage();
        openTemplateCreationDialog();
        selectTemplateJobType("ParameterDemoJob");
        fillTemplateName(ORIGINAL_TEMPLATE_NAME);
        fillTemplateParameters();
        submitTemplateCreationForm();

        originalTemplateId = extractTemplateIdFromTemplatesTable(ORIGINAL_TEMPLATE_NAME);
        System.out.println("Created original template ID: " + originalTemplateId);
        assertNotNull(originalTemplateId, "Original template ID should be extracted successfully");
    }

    @Test
    @Order(2)
    @DisplayName("Clone the template via UI")
    public void testCloneTemplateViaUI() {
        assertNotNull(originalTemplateId, "Original template ID should be set from previous test");

        navigateToTemplatesPage();

        // Find the template row and click the clone button
        Locator templateRow = page.locator("tr:has-text('" + ORIGINAL_TEMPLATE_NAME + "')");
        assertTrue(templateRow.isVisible(), "Original template should be visible");

        // Click the clone button (with bi-copy icon)
        Locator cloneButton = templateRow.locator("button[title='Klonen']");
        assertTrue(cloneButton.isVisible(), "Clone button should be visible");

        // Handle the confirmation dialog
        page.onDialog(dialog -> {
            System.out.println("Confirmation dialog: " + dialog.message());
            assertTrue(dialog.message().contains("wirklich klonen"), "Should show clone confirmation");
            dialog.accept();
        });

        cloneButton.click();

        // Wait for the table to refresh after cloning
        page.waitForTimeout(1000);

        // Expected clone name should be original name + current date
        String datePostfix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        expectedClonedTemplateName = ORIGINAL_TEMPLATE_NAME + "-" + datePostfix;
        System.out.println("Expected cloned template name: " + expectedClonedTemplateName);
    }

    @Test
    @Order(3)
    @DisplayName("Verify both original and cloned templates exist")
    public void testBothTemplatesExist() {
        assertNotNull(originalTemplateId, "Original template ID should be set");
        assertNotNull(expectedClonedTemplateName, "Expected cloned template name should be set");

        navigateToTemplatesPage();

        // Verify original template still exists (use exact match with strong tag)
        Locator originalTemplateRow = page.locator("tr:has(strong a:has-text('" + ORIGINAL_TEMPLATE_NAME + "'))").first();
        assertTrue(originalTemplateRow.isVisible(), "Original template should still exist");

        // Verify cloned template exists (use exact match with strong tag)
        Locator clonedTemplateRow = page.locator("tr:has(strong a:has-text('" + expectedClonedTemplateName + "'))").first();
        assertTrue(clonedTemplateRow.isVisible(), "Cloned template should exist");

        // Extract cloned template ID
        clonedTemplateId = extractTemplateIdFromTemplatesTable(expectedClonedTemplateName);
        System.out.println("Cloned template ID: " + clonedTemplateId);
        assertNotNull(clonedTemplateId, "Cloned template ID should be extracted successfully");

        // Verify the two templates have different IDs
        assertNotEquals(originalTemplateId, clonedTemplateId,
                "Original and cloned templates should have different IDs");
    }

    @Test
    @Order(4)
    @DisplayName("Verify cloned template has same job type as original")
    public void testClonedTemplateHasSameJobType() {
        assertNotNull(clonedTemplateId, "Cloned template ID should be set");

        navigateToTemplatesPage();

        // Both templates should have the same job type (use exact match with strong tag)
        Locator originalRow = page.locator("tr:has(strong a:has-text('" + ORIGINAL_TEMPLATE_NAME + "'))").first();
        Locator clonedRow = page.locator("tr:has(strong a:has-text('" + expectedClonedTemplateName + "'))").first();

        String originalRowText = originalRow.innerText();
        String clonedRowText = clonedRow.innerText();

        System.out.println("Original template row: " + originalRowText);
        System.out.println("Cloned template row: " + clonedRowText);

        assertTrue(originalRowText.contains("ParameterDemoJob"), "Original should have ParameterDemoJob type");
        assertTrue(clonedRowText.contains("ParameterDemoJob"), "Clone should have ParameterDemoJob type");
    }

    @Test
    @Order(5)
    @DisplayName("Verify cloned template can be edited and has same parameters")
    public void testClonedTemplateHasSameParameters() {
        assertNotNull(clonedTemplateId, "Cloned template ID should be set");

        navigateToTemplatesPage();

        // Open edit dialog for cloned template (use exact match with strong tag)
        Locator clonedRow = page.locator("tr:has(strong a:has-text('" + expectedClonedTemplateName + "'))").first();
        Locator editButton = clonedRow.locator("button[title='Bearbeiten']");
        editButton.click();

        page.waitForSelector("#jobModal.show", new Page.WaitForSelectorOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE));
        page.waitForSelector("#modal-content .spinner-border", new Page.WaitForSelectorOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));

        // Verify parameters match the original template values
        String stringParam = page.inputValue("input[name='parameters.stringParameter']");
        String intParam = page.inputValue("input[name='parameters.integerParameter']");
        String boolParam = page.inputValue("select[name='parameters.booleanParameter']");

        assertEquals("Template Test String", stringParam, "String parameter should match original");
        assertEquals("99", intParam, "Integer parameter should match original");
        assertEquals("true", boolParam, "Boolean parameter should match original");

        System.out.println("Verified cloned template has same parameters as original");

        // Close the modal
        page.click("button[data-bs-dismiss='modal']");
        page.waitForSelector("#jobModal.show", new Page.WaitForSelectorOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));
    }

    @Test
    @Order(6)
    @DisplayName("Clone the cloned template to verify reusability")
    public void testCloneTheClone() {
        assertNotNull(clonedTemplateId, "Cloned template ID should be set");

        navigateToTemplatesPage();

        // Find the cloned template and clone it again (use exact match with strong tag)
        Locator clonedTemplateRow = page.locator("tr:has(strong a:has-text('" + expectedClonedTemplateName + "'))").first();
        Locator cloneButton = clonedTemplateRow.locator("button[title='Klonen']");

        page.onDialog(dialog -> {
            dialog.accept();
        });

        cloneButton.click();
        page.waitForTimeout(1000);

        // Should now have 3 templates total
        navigateToTemplatesPage();
        // Count all rows that contain template names starting with the base name
        Locator originalRow = page.locator("tr:has(strong a:has-text('" + ORIGINAL_TEMPLATE_NAME + "'))").first();
        Locator firstCloneRow = page.locator("tr:has(strong a:has-text('" + expectedClonedTemplateName + "'))").first();

        assertTrue(originalRow.isVisible(), "Original template should exist");
        assertTrue(firstCloneRow.isVisible(), "First cloned template should exist");

        // Verify at least 3 templates exist (original + first clone + second clone)
        int totalTemplatesVisible = page.locator("tbody tr").count();
        System.out.println("Total templates visible: " + totalTemplatesVisible);
        assertTrue(totalTemplatesVisible >= 3, "Should have at least 3 templates (original + 2 clones)");
    }

    // Template-specific helper methods

    protected void navigateToTemplatesPage() {
        page.navigate(baseUrl + "q/jobrunr-control/templates");
        page.waitForSelector("h1:has-text('Template Jobs')");
    }

    protected void openTemplateCreationDialog() {
        page.click("button:has-text('Neues Template erstellen')");
        page.waitForSelector("#jobModal.show", new Page.WaitForSelectorOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE));
        page.waitForSelector("#modal-content .spinner-border", new Page.WaitForSelectorOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));
    }

    protected void selectTemplateJobType(String jobType) {
        page.selectOption("select[name='jobType']", jobType);
        page.waitForFunction("!document.querySelector('#parameters-container').textContent.includes('Wählen Sie einen Job aus')");
    }

    protected void fillTemplateName(String templateName) {
        page.fill("input[name='jobName']", templateName);
    }

    protected void fillTemplateParameters() {
        page.fill("input[name='parameters.stringParameter']", "Template Test String");
        page.fill("input[name='parameters.integerParameter']", "99");
        page.selectOption("select[name='parameters.booleanParameter']", "true");

        String dateValue = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        page.fill("input[name='parameters.dateParameter']", dateValue);

        String dateTimeValue = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        page.fill("input[name='parameters.dateTimeParameter']", dateTimeValue);

        page.selectOption("select[name='parameters.enumParameter']", "OPTION_A");
    }

    protected void submitTemplateCreationForm() {
        page.click("button[type='submit']:has-text('Template erstellen')");
        page.waitForSelector("#jobModal.show", new Page.WaitForSelectorOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));
    }

    protected UUID extractTemplateIdFromTemplatesTable(String templateName) {
        Locator templateLink = page.locator("strong a:has-text('" + templateName + "')").first();
        templateLink.waitFor(new Locator.WaitForOptions().setTimeout(5000));
        assertTrue(templateLink.isVisible(), "Template link should appear in the templates table");

        String href = templateLink.getAttribute("href");
        assertNotNull(href, "Template link href should be present");
        System.out.println("Template link href: " + href);

        String[] hrefParts = href.split("/");
        String templateIdString = hrefParts[hrefParts.length - 1];
        assertNotNull(templateIdString, "Template ID should be extractable from href");
        return UUID.fromString(templateIdString);
    }
}
