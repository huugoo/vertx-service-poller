package se.kry.codetest;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

    // TODO: Create a separate database file test.db
    @BeforeEach
    void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
        DBConnector connector = new DBConnector(vertx);
        //TODO: Create-recreate the db instead
        connector.query("DELETE from service where name like '%'").setHandler(asyncResult -> {
            if(asyncResult.succeeded()) {
                vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
            }
        });
    }

    @Test
    @DisplayName("Start a web server on localhost responding to path /service on port 8080")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void start_http_server(Vertx vertx, VertxTestContext testContext) {
    WebClient.create(vertx)
        .get(8080, "::1", "/service")
        .send(response -> testContext.verify(() -> {
          assertEquals(200, response.result().statusCode());
          testContext.completeNow();
        }));
    }

    @Test
    @DisplayName("Add two new services with two posts to /service")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void add_service(Vertx vertx, VertxTestContext testContext) {
        final String serviceName1 = "test_Name1";
        final String serviceName2 = "test_Name2";
        final String url = "http://www.kry.se";

        // add a new service
        JsonObject json = new JsonObject().put("name", serviceName1).put("url", url);
        WebClient.create(vertx)
                .post(8080, "::1", "/service")
                .sendJsonObject(json, response -> testContext.verify(() -> {
                    assertEquals(200, response.result().statusCode());
                    String body = response.result().bodyAsString();
                    assertEquals("OK", body);

                    // add a second new service
                    JsonObject json2 = new JsonObject().put("name", serviceName2).put("url", url);
                    WebClient.create(vertx)
                            .post(8080, "::1", "/service")
                            .sendJsonObject(json2, respons2 -> testContext.verify(() -> {
                                assertEquals(200, respons2.result().statusCode());
                                String body2 = respons2.result().bodyAsString();
                                assertEquals("OK", body);

                                // is the new service created?
                                WebClient.create(vertx)
                                        .get(8080, "::1", "/service")
                                        .send(response3 -> testContext.verify(() -> {
                                            assertEquals(200, response3.result().statusCode());
                                            JsonArray body3 = response3.result().bodyAsJsonArray();
                                            System.out.println("list of services: " + body3);
                                            assertEquals(2, body3.size());
                                            // TODO: make a loop and verify both Jsons in the array
                                            JsonObject service = body3.getJsonObject(1);
                                            assertEquals(serviceName1, service.getString("name"));
                                            assertEquals(url, service.getString("url"));
                                            assertEquals(Status.UNKNOWN.toString(), service.getString("status"));
                                            testContext.completeNow();
                                        }));
                            }));
                }));
    }

    @Test
    @DisplayName("Bad url post to /service")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void bad_url(Vertx vertx, VertxTestContext testContext) {
        final String serviceName = "test_Name1";
        final String url = "afdase.asdf";

        // add a new service
        JsonObject json = new JsonObject().put("name", serviceName).put("url", url);
        WebClient.create(vertx)
                .post(8080, "::1", "/service")
                .sendJsonObject(json, response -> testContext.verify(() -> {
                    assertEquals(400, response.result().statusCode());
                    testContext.completeNow();
                }));
    }

    @Test
    @DisplayName("Post and Delete a service")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void post_and_delete_service(Vertx vertx, VertxTestContext testContext) {
        final String serviceName = "test_Name1";
        final String url = "http://www.kry.se";
        JsonObject json = new JsonObject().put("name", serviceName).put("url", url);
        WebClient.create(vertx)
                .post(8080, "::1", "/service")
                .sendJsonObject(json, response -> testContext.verify(() -> {
                    assertEquals(200, response.result().statusCode());
                    String body = response.result().bodyAsString();
                    assertEquals("OK", body);

                    // is the new service created?
                    WebClient.create(vertx)
                            .get(8080, "::1", "/service")
                            .send(response2 -> testContext.verify(() -> {
                                    assertEquals(200, response2.result().statusCode());
                                    JsonArray body2 = response2.result().bodyAsJsonArray();
                                    System.out.println("list of services: " + body2);
                                    assertEquals(1, body2.size());
                                    JsonObject service = body2.getJsonObject(0);
                                    assertEquals(serviceName, service.getString("name"));
                                    assertEquals(url, service.getString("url"));
                                    assertEquals(Status.UNKNOWN.toString(), service.getString("status"));

                                // delete the service
                                WebClient.create(vertx)
                                        .delete(8080, "::1", "/service/" + serviceName)
                                        .send(response3 -> testContext.verify(() -> {
                                            assertEquals(200, response3.result().statusCode());
                                            String body3 = response3.result().bodyAsString();
                                            assertEquals("OK", body3);

                                            // is the service deleted?
                                            WebClient.create(vertx)
                                                    .get(8080, "::1", "/service")
                                                    .send(response4 -> testContext.verify(() -> {
                                                        assertEquals(200, response4.result().statusCode());
                                                        JsonArray body4 = response4.result().bodyAsJsonArray();
                                                        System.out.println("services after deletion: " + body4);
                                                        assertEquals(0, body4.size());
                                                        testContext.completeNow();
                                                    }));
                                        }));
                            }));
                }));
    }
}
