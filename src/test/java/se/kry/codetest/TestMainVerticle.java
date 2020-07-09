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

    // TODO: Create a separate test.db
    @BeforeEach
    void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
        DBConnector connector = new DBConnector(vertx);
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
          // JsonArray body = response.result().bodyAsJsonArray();
          // assertEquals(1, body.size());
          testContext.completeNow();
        }));
    }

    @Test
    @DisplayName("Add two new services with two posts to /service")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void add_service(Vertx vertx, VertxTestContext testContext) {
        // add a new service
        JsonObject json = new JsonObject().put("name", "testName1").put("url", "kry.se");
        WebClient.create(vertx)
                .post(8080, "::1", "/service")
                .sendJsonObject(json, response -> testContext.verify(() -> {
                    assertEquals(200, response.result().statusCode());
                    String body = response.result().bodyAsString();
                    System.out.println("post body: " + body);
                    assertEquals("OK", body);
                    testContext.completeNow();
                }));

        // add a second new service
        JsonObject json2 = new JsonObject().put("name", "testName2").put("url", "http://www.kry.se");
        WebClient.create(vertx)
                .post(8080, "::1", "/service")
                .sendJsonObject(json, response -> testContext.verify(() -> {
                    assertEquals(200, response.result().statusCode());
                    String body = response.result().bodyAsString();
                    System.out.println("post body: " + body);
                    assertEquals("OK", body);
                    testContext.completeNow();
                }));

        // is the new service created?
        WebClient.create(vertx)
                .get(8080, "::1", "/service")
                .send(response -> testContext.verify(() -> {
                    assertEquals(200, response.result().statusCode());
                    JsonArray body = response.result().bodyAsJsonArray();
                    System.out.println("get body: " + body);
                    assertEquals(2, body.size());
                    JsonObject service = body.getJsonObject(1);
                    assertEquals("testName2", service.getString("name"));
                    assertEquals("http://www.kry.se", service.getString("url"));
                    assertEquals(Status.UNKOWN, service.getString("status"));
                    testContext.completeNow();
                }));
    }

    @Test
    @DisplayName("Post and Delete a service")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void post_and_delete_service(Vertx vertx, VertxTestContext testContext) {
        final String service_name = "testName1";
        // add a new service
        JsonObject json = new JsonObject().put("name", service_name).put("url", "kry.se");
        WebClient.create(vertx)
                .post(8080, "::1", "/service")
                .sendJsonObject(json, response -> testContext.verify(() -> {
                    assertEquals(200, response.result().statusCode());
                    String body = response.result().bodyAsString();
                    System.out.println("post body: " + body);
                    assertEquals("OK", body);

                    // is the new service created?
                    WebClient.create(vertx)
                            .get(8080, "::1", "/service")
                            .send(response2 -> testContext.verify(() -> {
                                    assertEquals(200, response2.result().statusCode());
                                    JsonArray body2 = response2.result().bodyAsJsonArray();
                                    System.out.println("get body2: " + body2);
                                    assertEquals(1, body2.size());
                                    JsonObject service = body2.getJsonObject(0);
                                    assertEquals(service_name, service.getString("name"));
                                    assertEquals("kry.se", service.getString("url"));
                                    assertEquals(Status.UNKOWN.toString(), service.getString("status"));

                                // delete the service
                                WebClient.create(vertx)
                                        .delete(8080, "::1", "/service/" + service_name)
                                        .send(response3 -> testContext.verify(() -> {
                                            assertEquals(200, response3.result().statusCode());
                                            String body3 = response3.result().bodyAsString();
                                            System.out.println("post body: " + body3);
                                            assertEquals("OK", body3);
                                            //testContext.completeNow();

                                            // is the service deleted?
                                            WebClient.create(vertx)
                                                    .get(8080, "::1", "/service")
                                                    .send(response4 -> testContext.verify(() -> {
                                                        assertEquals(200, response4.result().statusCode());
                                                        JsonArray body4 = response4.result().bodyAsJsonArray();
                                                        System.out.println("get body: " + body4);
                                                        assertEquals(0, body4.size());
                                                        testContext.completeNow();
                                                    }));
                                        }));
                            }));
                }));
    }
}
