package ch.css.jobrunr.control.ui;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.quarkiverse.playwright.InjectPlaywright;
import io.quarkiverse.playwright.WithPlaywright;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base class for JobRunr Control UI tests using Playwright.
 * Provides common navigation and interaction methods for job management tests.
 * Use @WithPlaywright(browser = WithPlaywright.Browser.CHROMIUM, headless = false, slowMo = 1000) for debugging.
 */
@WithPlaywright
//@WithPlaywright(browser = WithPlaywright.Browser.CHROMIUM, headless = false, slowMo = 1000)
public abstract class JobTriggerUITestBase {

    @InjectPlaywright
    protected BrowserContext context;

    @TestHTTPResource("/")
    protected URL baseUrl;

    protected Page page;
    protected static UUID scheduledJobId;

    @BeforeEach
    public void setUp() {
        page = context.newPage();
    }

    @AfterEach
    public void tearDown() {
        if (page != null) {
            page.close();
        }
    }

    // Navigation methods

    protected void navigateToScheduledJobsPage() {
        page.navigate(baseUrl + "q/jobrunr-control/scheduled");
        page.waitForSelector("h1:has-text('Geplante Jobs')");
    }

    protected void navigateToHistory() {
        page.navigate(baseUrl + "q/jobrunr-control/history");
        page.waitForSelector("h1:has-text('Ausführungshistorie')");
    }

    // Job creation methods

    protected void openJobCreationDialog() {
        page.click("button:has-text('Neuen Job erstellen')");
        page.waitForSelector("#jobModal.show", new Page.WaitForSelectorOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE));
        page.waitForSelector("#modal-content .spinner-border", new Page.WaitForSelectorOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));
    }

    protected void selectJobType(String jobType) {
        page.selectOption("select[name='jobType'][hx-target='#parameters-container']", jobType);
        page.waitForFunction("!document.querySelector('#parameters-container').textContent.includes('Wählen Sie einen Job aus')");
    }

    protected void fillJobName(String jobName) {
        page.fill("input[name='jobName']", jobName);
    }

    protected void enableExternalTrigger() {
        Locator externalTriggerCheckbox = page.locator("input[id='triggerExternal']");
        if (!externalTriggerCheckbox.isChecked()) {
            externalTriggerCheckbox.check();
        }
    }

    protected void submitJobCreationForm() {
        page.click("button[type='submit']:has-text('Job Erstellen')");
        page.waitForSelector("#jobModal.show", new Page.WaitForSelectorOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));
    }

    // Job extraction and verification methods

    protected UUID extractJobIdFromScheduledJobsTable(String jobName) {
        Locator jobLink = page.locator("strong a:has-text('" + jobName + "')");
        jobLink.waitFor(new Locator.WaitForOptions().setTimeout(5000));
        assertTrue(jobLink.isVisible(), "Job link should appear in the scheduled jobs table");

        String href = jobLink.getAttribute("href");
        assertNotNull(href, "Job link href should be present");
        System.out.println("Job link href: " + href);

        String[] hrefParts = href.split("/");
        String jobIdString = hrefParts[hrefParts.length - 1];
        assertNotNull(jobIdString, "Job ID should be extractable from href");
        return UUID.fromString(jobIdString);
    }

    // API interaction methods

    protected String triggerJobViaApi(UUID jobId) {
        String requestBody = """
                {
                    "test": "true"
                }
                """;

        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post(baseUrl + "q/jobrunr-control/api/jobs/" + jobId + "/start")
                .then()
                .statusCode(200)
                .extract()
                .asString();
    }

    // History verification methods

    protected void searchForJob(String jobName) {
        page.fill("input[name='search']", jobName);
    }

    protected void verifyJobInHistory(String jobName, String expectedJobType) {
        Locator jobLinkInHistory = page.locator("a.text-decoration-none strong:has-text('" + jobName + "')");
        jobLinkInHistory.waitFor(new Locator.WaitForOptions().setTimeout(5000));
        assertTrue(jobLinkInHistory.isVisible(), "Job link should appear in the history table");

        Locator historyRow = page.locator("tr:has-text('" + jobName + "')");
        assertTrue(historyRow.isVisible(), "Job should appear in execution history");

        String rowText = historyRow.innerText();
        System.out.println("Job execution history row: " + rowText);
        assertTrue(
                rowText.contains("SUCCEEDED") || rowText.contains("PROCESSING") || rowText.contains("ENQUEUED"),
                "Job should have a valid execution status"
        );

        String rowHtml = historyRow.innerHTML();
        assertTrue(rowHtml.contains(expectedJobType) || rowHtml.contains(jobName),
                "Job name should be visible in history");
    }

    // Template-specific methods

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
        page.selectOption("select[name='jobType'][hx-target='#parameters-container']", jobType);
        page.waitForFunction("!document.querySelector('#parameters-container').textContent.includes('Wählen Sie einen Job aus')");
    }

    protected void fillTemplateName(String templateName) {
        page.fill("input[name='jobName']", templateName);
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

    // Parameter filling helper methods

    /**
     * Fills ParameterDemoJob parameters with custom values.
     */
    protected void fillParameterDemoJobParameters(
            String stringValue,
            String intValue,
            String booleanValue,
            String enumValue) {

        page.fill("input[name='parameters.stringParameter']", stringValue);
        page.fill("input[name='parameters.integerParameter']", intValue);
        page.selectOption("select[name='parameters.booleanParameter']", booleanValue);

        // Date fields with current date
        String dateValue = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        page.fill("input[name='parameters.dateParameter']", dateValue);

        String dateTimeValue = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        page.fill("input[name='parameters.dateTimeParameter']", dateTimeValue);

        page.selectOption("select[name='parameters.enumParameter']", enumValue);
    }

    /**
     * Fills ParameterDemoJob parameters with default test values.
     */
    protected void fillParameterDemoJobParametersWithDefaults() {
        fillParameterDemoJobParameters("Test String Value", "123", "false", "OPTION_A");
        // Add multi-select enum if present
        page.selectOption("select[name='parameters.multiEnumParameter']", new String[]{"OPTION_A", "OPTION_C"});
    }

    /**
     * Fills batch job parameters with custom values.
     */
    protected void fillBatchJobParameters(int numberOfChunks, int chunkSize, boolean simulateErrors) {
        page.fill("input[name='parameters.numberOfChunks']", String.valueOf(numberOfChunks));
        page.fill("input[name='parameters.chunkSize']", String.valueOf(chunkSize));
        page.selectOption("select[name='parameters.simulateErrors']", String.valueOf(simulateErrors));
    }

    /**
     * Fills batch job parameters with default test values.
     */
    protected void fillBatchJobParametersWithDefaults() {
        fillBatchJobParameters(10, 50, false);
    }
}
