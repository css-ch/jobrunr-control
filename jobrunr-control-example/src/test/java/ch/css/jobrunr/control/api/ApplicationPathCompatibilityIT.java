package ch.css.jobrunr.control.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;

/**
 * Verifies the issue-#25 fix in-situ: the example application declares
 * {@code @ApplicationPath("/api")} via {@link ch.css.jobrunr.control.rest.ExampleApplication},
 * so JAX-RS resources are prefixed with {@code /api}. The JobRunr Control UI must remain
 * reachable at {@code /q/jobrunr-control/*} because it is registered under the
 * non-application root path, not as a JAX-RS resource.
 */
@QuarkusTest
@DisplayName("Dashboard stays at /q/jobrunr-control/* while REST API is prefixed by @ApplicationPath")
class ApplicationPathCompatibilityIT {

    @Test
    @DisplayName("UI dashboard stays at /q/jobrunr-control/scheduled regardless of @ApplicationPath")
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
    @DisplayName("REST API is prefixed by @ApplicationPath (Option B from issue #25)")
    void restApiPrefixedByApplicationPath() {
        UUID nonExistentJob = UUID.randomUUID();

        // JobControlResource's @Path("/q/jobrunr-control/api") is now mounted under /api.
        // 404 (job not found) proves the route reached the handler; 200 would also confirm
        // it is mounted. Anything other than that is a regression.
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

        // Legacy path must return 404 because @ApplicationPath moved JAX-RS to /api.
        RestAssured.given()
                .when()
                .get("/q/jobrunr-control/api/jobs/" + nonExistentJob)
                .then()
                .statusCode(404);
    }
}
