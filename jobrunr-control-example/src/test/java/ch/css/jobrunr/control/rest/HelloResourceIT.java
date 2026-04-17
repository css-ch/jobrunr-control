package ch.css.jobrunr.control.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;

/**
 * Integration test for {@link HelloResource}. Verifies that JAX-RS resources are served under
 * the {@link jakarta.ws.rs.ApplicationPath} declared on {@link ExampleApplication} ({@code /api})
 * and that paths without that prefix are not reachable.
 */
@QuarkusTest
@DisplayName("HelloResource is mounted under @ApplicationPath(\"/api\")")
class HelloResourceIT {

    @Test
    @DisplayName("GET /api/hello returns the greeting")
    void helloReachableUnderApplicationPath() {
        RestAssured.given()
                .when()
                .get("/api/hello")
                .then()
                .statusCode(200)
                .body(equalTo("Hello from JobRunr Control example"));
    }

    @Test
    @DisplayName("GET /hello (without @ApplicationPath prefix) returns 404")
    void helloNotReachableWithoutApplicationPath() {
        RestAssured.given()
                .when()
                .get("/hello")
                .then()
                .statusCode(404);
    }
}
