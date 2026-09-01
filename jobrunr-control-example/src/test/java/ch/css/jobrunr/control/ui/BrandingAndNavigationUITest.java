package ch.css.jobrunr.control.ui;

import com.microsoft.playwright.Locator;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(BrandingAndNavigationUITest.UiExtensionsProfile.class)
class BrandingAndNavigationUITest extends JobTriggerUITestBase {

    @Test
    @DisplayName("Renders custom branding and stage")
    void rendersCustomBrandingAndStage() {
        page.navigate(baseUrl + "q/jobrunr-control/history");

        page.waitForSelector("a.navbar-brand");

        Locator branding = page.locator("a.navbar-brand");
        assertTrue(branding.innerText().contains("Print Dashboard"));
        assertEquals("/api/hello", branding.getAttribute("href"));

        Locator stage = page.locator(".stage-badge");
        assertTrue(stage.innerText().contains("DEV"));
    }

    @Test
    @DisplayName("Renders custom and active navigation")
    void rendersCustomAndActiveNavigation() {
        page.navigate(baseUrl + "q/jobrunr-control/history");

        Locator customNavigation = page.locator("a.nav-link")
                .filter(new Locator.FilterOptions().setHasText("Google-Suche"));
        assertTrue(customNavigation.isVisible());
        assertEquals(
                "https://www.google.com/search?q=JobRunr+Pro",
                customNavigation.getAttribute("href"));

        assertTrue(page.locator("a.nav-link.active")
                .allInnerTexts()
                .stream()
                .anyMatch(text -> text.contains("Historie")));
    }

    public static class UiExtensionsProfile implements QuarkusTestProfile {

        @Override
        public String getConfigProfile() {
            return "ui-extensions";
        }
    }
}
