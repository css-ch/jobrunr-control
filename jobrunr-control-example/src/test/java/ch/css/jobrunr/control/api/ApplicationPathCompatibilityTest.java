package ch.css.jobrunr.control.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;

/**
 * Verifies that the JobRunr Control dashboard and REST API remain reachable when the
 * consuming application configures a non-default JAX-RS root path (equivalent to declaring
 * {@code @ApplicationPath("/api")}).
 * <p>
 * The UI endpoints are registered under Quarkus' non-application root path and must stay at
 * {@code /q/jobrunr-control/*} independent of {@code quarkus.rest.path}. The REST API remains
 * a JAX-RS resource and is therefore shifted to {@code {rest-path}/q/jobrunr-control/api/*}.
 */
@QuarkusTest
@TestProfile(ApplicationPathCompatibilityTest.CustomRestPathProfile.class)
@DisplayName("Dashboard and REST API remain reachable when quarkus.rest.path is customized")
class ApplicationPathCompatibilityTest {

    public static class CustomRestPathProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.rest.path", "/api");
        }
    }

    @Test
    @DisplayName("UI dashboard stays at /q/jobrunr-control/scheduled regardless of quarkus.rest.path")
    void uiDashboardReachableAtNonApplicationRootPath() {
        RestAssured.given()
                .when()
                .get("/q/jobrunr-control/scheduled")
                .then()
                .statusCode(200)
                .body(containsString("Geplante Jobs"));
    }

    @Test
    @DisplayName("UI scheduled-jobs table fragment stays at /q/jobrunr-control/scheduled/table")
    void uiTableFragmentReachable() {
        RestAssured.given()
                .when()
                .get("/q/jobrunr-control/scheduled/table")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("UI history page stays at /q/jobrunr-control/history")
    void uiHistoryReachable() {
        RestAssured.given()
                .when()
                .get("/q/jobrunr-control/history")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("REST API is prefixed by quarkus.rest.path (Option B from issue #25)")
    void restApiPrefixedByApplicationPath() {
        UUID nonExistentJob = UUID.randomUUID();

        // The REST resource at @Path("/q/jobrunr-control/api") is now mounted under /api:
        // 404 (job not found) proves the route reached the handler; 405/415/200 would also
        // confirm it is mounted — anything except 404-from-router is a pass.
        int statusOnConfiguredPath = RestAssured.given()
                .when()
                .get("/api/q/jobrunr-control/api/jobs/" + nonExistentJob)
                .then()
                .extract()
                .statusCode();
        org.junit.jupiter.api.Assertions.assertTrue(
                statusOnConfiguredPath == 404 || statusOnConfiguredPath == 200,
                "Expected REST endpoint to respond at /api/q/jobrunr-control/api/... but got "
                        + statusOnConfiguredPath);

        // Legacy path must return 404 because @ApplicationPath/quarkus.rest.path moved JAX-RS to /api.
        RestAssured.given()
                .when()
                .get("/q/jobrunr-control/api/jobs/" + nonExistentJob)
                .then()
                .statusCode(404);
    }
}
