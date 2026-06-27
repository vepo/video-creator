package dev.vepo.youtube.creator.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import dev.vepo.youtube.creator.support.ScenarioContext;
import jakarta.inject.Inject;

public class HealthApiSteps {

    @Inject
    ScenarioContext context;

    @When("I request the health check")
    public void requestHealthCheck() {
        context.setLastResponse(
                RestAssured.given().when().get("/api/video/health"));
    }

    @Then("the response status should be {int}")
    public void responseStatusShouldBe(int status) {
        assertEquals(status, context.getLastResponse().statusCode());
    }

    @Then("the health response should include a status field")
    public void healthResponseShouldIncludeStatus() {
        assertNotNull(context.getLastResponse().jsonPath().getString("status"));
    }

    @Then("the health response should include a melt availability message")
    public void healthResponseShouldIncludeMessage() {
        assertNotNull(context.getLastResponse().jsonPath().getString("message"));
    }
}
