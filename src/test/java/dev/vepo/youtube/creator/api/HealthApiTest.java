package dev.vepo.youtube.creator.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.shared.QuarkusIntegrationTest;

@QuarkusIntegrationTest
class HealthApiTest {

    @Test
    void healthCheckReturnsStatusAndMeltMessage() {
        given()
                .when()
                .get("/api/video/health")
                .then()
                .statusCode(200)
                .body("status", notNullValue())
                .body("message", notNullValue());
    }
}
