package ch.css.jobrunr.control.ui;

import com.microsoft.playwright.Locator;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI test for creating a template job, executing it via REST API, and verifying:
 * - Template is created and visible in Templates tab
 * - Template can be executed (cloned)
 * - Execution appears in history
 * - Template remains unchanged and available for reuse
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TemplateJobUITest extends JobTriggerUITestBase {

    private static UUID templateId;
    private static UUID executedJobId;
    private static final String TEMPLATE_NAME = "Test Template - ParameterDemo";
    private static final String EXECUTION_POSTFIX = "20260127-test";
    private static final String EXPECTED_EXECUTED_JOB_NAME = TEMPLATE_NAME + "-" + EXECUTION_POSTFIX;


    @Test
    @Order(1)
    @DisplayName("Create a template job via Templates UI")
    public void testCreateTemplate() {
        navigateToTemplatesPage();
        openTemplateCreationDialog();
        selectTemplateJobType("ParameterDemoJob");
        fillTemplateName(TEMPLATE_NAME);
        fillTemplateParameters();
        submitTemplateCreationForm();

        templateId = extractTemplateIdFromTemplatesTable(TEMPLATE_NAME);
        System.out.println("Created template ID: " + templateId);
        assertNotNull(templateId, "Template ID should be extracted successfully");
    }

    @Test
    @Order(2)
    @DisplayName("Verify template is visible in Templates tab")
    public void testTemplateIsVisible() {
        assertNotNull(templateId, "Template ID should be set from previous test");

        navigateToTemplatesPage();

        Locator templateRow = page.locator("tr:has-text('" + TEMPLATE_NAME + "')");
        assertTrue(templateRow.isVisible(), "Template should be visible in templates table");

        String rowText = templateRow.innerText();
        System.out.println("Template row: " + rowText);
        assertTrue(rowText.contains("ParameterDemoJob"), "Template should show correct job type");
    }

    @Test
    @Order(3)
    @DisplayName("Execute template via REST API")
    public void testExecuteTemplateViaRest() {
        assertNotNull(templateId, "Template ID should be set from previous test");

        String response = executeTemplateViaApi(templateId, EXECUTION_POSTFIX);
        System.out.println("Template execution response: " + response);

        assertTrue(response.contains("Template job started successfully"),
                "Template should be executed successfully");

        // Extract the new job ID from response
        executedJobId = extractJobIdFromResponse(response);
        System.out.println("Executed job ID: " + executedJobId);
        assertNotNull(executedJobId, "Executed job ID should be present in response");
    }

    @Test
    @Order(4)
    @DisplayName("Verify executed job appears in history")
    public void testExecutedJobInHistory() {
        assertNotNull(executedJobId, "Executed job ID should be set from previous test");

        navigateToHistory();
        searchForJob(EXPECTED_EXECUTED_JOB_NAME);

        Locator executedJobRow = page.locator("tr:has-text('" + EXPECTED_EXECUTED_JOB_NAME + "')");
        executedJobRow.waitFor(new Locator.WaitForOptions().setTimeout(5000));
        assertTrue(executedJobRow.isVisible(), "Executed job should appear in history");

        String rowText = executedJobRow.innerText();
        System.out.println("Executed job in history: " + rowText);
        assertTrue(
                rowText.contains("SUCCEEDED") || rowText.contains("PROCESSING") || rowText.contains("ENQUEUED"),
                "Executed job should have a valid execution status"
        );
    }

    @Test
    @Order(5)
    @DisplayName("Verify template is still available in Templates tab (unchanged)")
    public void testTemplateStillAvailable() {
        assertNotNull(templateId, "Template ID should be set from previous test");

        navigateToTemplatesPage();

        Locator templateRow = page.locator("tr:has-text('" + TEMPLATE_NAME + "')");
        assertTrue(templateRow.isVisible(), "Template should still be visible after execution");

        String rowText = templateRow.innerText();
        System.out.println("Template row after execution: " + rowText);

        // Verify template name is exactly as created (not changed with postfix)
        Locator templateLink = page.locator("strong a:has-text('" + TEMPLATE_NAME + "')");
        assertTrue(templateLink.isVisible(), "Template should have its original name");

        // Verify it's still a template (not converted to executed job)
        assertTrue(rowText.contains("ParameterDemoJob"), "Template should still show correct job type");
    }

    @Test
    @Order(6)
    @DisplayName("Execute template again to verify reusability")
    public void testTemplateReusability() {
        assertNotNull(templateId, "Template ID should be set from previous test");

        String secondPostfix = "20260127-test2";
        String response = executeTemplateViaApi(templateId, secondPostfix);

        assertTrue(response.contains("Template job started successfully"),
                "Template should be executable multiple times");

        UUID secondExecutedJobId = extractJobIdFromResponse(response);
        System.out.println("Second execution job ID: " + secondExecutedJobId);
        assertNotNull(secondExecutedJobId, "Second execution should create a new job");
        assertNotEquals(executedJobId, secondExecutedJobId,
                "Each execution should create a unique job");

        // Verify second execution is in history
        navigateToHistory();
        String secondJobName = TEMPLATE_NAME + "-" + secondPostfix;
        searchForJob(secondJobName);

        Locator secondJobRow = page.locator("tr:has-text('" + secondJobName + "')");
        secondJobRow.waitFor(new Locator.WaitForOptions().setTimeout(5000));
        assertTrue(secondJobRow.isVisible(), "Second execution should appear in history");
    }

    // Template-specific parameters filling (specific to this test)

    protected void fillTemplateParameters() {
        page.fill("input[name='parameters.stringParameter']", "Template Default String");
        page.fill("input[name='parameters.integerParameter']", "42");
        page.selectOption("select[name='parameters.booleanParameter']", "true");

        String dateValue = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        page.fill("input[name='parameters.dateParameter']", dateValue);

        String dateTimeValue = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        page.fill("input[name='parameters.dateTimeParameter']", dateTimeValue);

        page.selectOption("select[name='parameters.enumParameter']", "OPTION_B");
    }

    // Template execution API methods (specific to this test)

    protected String executeTemplateViaApi(UUID templateId, String postfix) {
        String requestBody = String.format("""
                {
                    "postfix": "%s",
                    "parameters": {
                        "stringParameter": "Overridden String"
                    }
                }
                """, postfix);

        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post(baseUrl + "q/jobrunr-control/api/templates/" + templateId + "/start")
                .then()
                .statusCode(200)
                .extract()
                .asString();
    }

    protected UUID extractJobIdFromResponse(String jsonResponse) {
        // Simple extraction - assumes response format: {"jobId":"uuid","message":"..."}
        int jobIdStart = jsonResponse.indexOf("\"jobId\":\"") + 9;
        int jobIdEnd = jsonResponse.indexOf("\"", jobIdStart);
        String jobIdString = jsonResponse.substring(jobIdStart, jobIdEnd);
        return UUID.fromString(jobIdString);
    }
}
