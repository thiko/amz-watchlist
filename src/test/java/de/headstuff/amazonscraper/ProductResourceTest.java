package de.headstuff.amazonscraper;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProductResourceTest {

  // TODO: think about Testcontainers with Mongo DB running

  @Test
  void testProductsEndpoint() {
    given()
        .when().get("/products")
        .then()
        .statusCode(200);
  }

}