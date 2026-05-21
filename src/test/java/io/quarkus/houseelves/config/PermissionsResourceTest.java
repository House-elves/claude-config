package io.quarkus.houseelves.config;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class PermissionsResourceTest {

    @Test
    void mergedPermissionsReturnsArray() {
        given()
            .when().get("/api/permissions/merged")
            .then()
            .statusCode(200)
            .body("$", is(not(empty())))
            .body("[0].pattern", notNullValue())
            .body("[0].level", notNullValue())
            .body("[0].stale", notNullValue());
    }

    @Test
    void stalePermissionsReturnsGrouped() {
        given()
            .when().get("/api/permissions/stale")
            .then()
            .statusCode(200)
            .body("stale", notNullValue())
            .body("grouped", notNullValue());
    }

    @Test
    void addRequiresPattern() {
        given()
            .contentType("application/json")
            .body("{\"level\":\"user\",\"pattern\":\"\"}")
            .when().post("/api/permissions/add")
            .then()
            .statusCode(400);
    }
}
