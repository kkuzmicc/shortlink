package com.shortlink.links;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;

@QuarkusTest
class LinkResourceTest {
    @Test
    void createsAndListsLinksInPostgres() {
        String target = "https://example.com/articles/first";

        String code = given()
                .contentType("application/json")
                .body("{\"targetUrl\":\"" + target + "\",\"title\":\"First article\"}")
                .when().post("/api/links")
                .then()
                .statusCode(201)
                .body("code", matchesPattern("[0-9A-Za-z]{7}"))
                .body("targetUrl", equalTo(target))
                .body("status", equalTo("ACTIVE"))
                .extract().path("code");

        given().when().get("/api/links?limit=100")
                .then().statusCode(200)
                .body("code", hasItem(code));
    }

    @Test
    void rejectsInvalidTargetsAndDuplicateAliases() {
        given().contentType("application/json")
                .body("{\"targetUrl\":\"javascript:alert(1)\"}")
                .when().post("/api/links")
                .then().statusCode(400);

        String alias = "test" + UUID.randomUUID().toString().substring(0, 8);

        String body = "{\"targetUrl\":\"https://example.com\",\"customAlias\":\"" + alias + "\"}";

        given().contentType("application/json").body(body)
                .when().post("/api/links")
                .then().statusCode(201).body("code", equalTo(alias));

        given().contentType("application/json").body(body)
                .when().post("/api/links")
                .then().statusCode(409);
    }
}
