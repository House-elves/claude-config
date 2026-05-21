package io.quarkus.houseelves.config;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class AppConfigResourceTest {

    @Test
    void projectDiscoveryReturnsProjects() {
        given()
            .when().get("/api/projects")
            .then()
            .statusCode(200)
            .body("$", is(not(empty())))
            .body("[0].name", notNullValue())
            .body("[0].path", notNullValue());
    }

    @Test
    void appConfigReturnsDefaults() {
        given()
            .when().get("/api/app-config")
            .then()
            .statusCode(200)
            .body("PROJECT_PATHS", notNullValue());
    }

    @Test
    void settingsUserReturnsJson() {
        given()
            .when().get("/api/settings/user")
            .then()
            .statusCode(200)
            .body("model", notNullValue());
    }

    @Test
    void claudeMdUserReturnsText() {
        given()
            .when().get("/api/claude-md/user")
            .then()
            .statusCode(200)
            .contentType(containsString("text/plain"));
    }

    @Test
    void mcpUserReturnsJson() {
        given()
            .when().get("/api/mcp/user")
            .then()
            .statusCode(200)
            .body("mcpServers", notNullValue());
    }

    @Test
    void commandsUserReturnsList() {
        given()
            .when().get("/api/commands/user")
            .then()
            .statusCode(200)
            .body("$", is(not(empty())));
    }

    @Test
    void keybindingsReturnsJson() {
        given()
            .when().get("/api/keybindings")
            .then()
            .statusCode(200);
    }
}
