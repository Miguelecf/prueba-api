package org.migue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class PostResourceTest {

    @Test
    void testGetAllPosts() {
        given()
                .when().get("/posts")
                .then()
                .statusCode(200)
                .body("$", not(empty()))
                // Verifica que el primer post tenga los campos esperados
                .body("[0].id", notNullValue())
                .body("[0].title", notNullValue())
                .body("[0].authorName", notNullValue());
    }

    @Test
    void testDeletePost() {
        given()
                .when().delete("/posts/1")
                .then()
                .statusCode(204); // RESTful: No Content al eliminar
    }
}

