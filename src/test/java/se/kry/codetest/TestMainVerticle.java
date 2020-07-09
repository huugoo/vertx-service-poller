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

    @BeforeEach
    void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
    }

    @Test
    @DisplayName("Start a web server on localhost responding to path /service on port 8080")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void start_http_server(Vertx vertx, VertxTestContext testContext) {
    WebClient.create(vertx)
        .get(8080, "::1", "/service")
        .send(response -> testContext.verify(() -> {
          assertEquals(200, response.result().statusCode());
          JsonArray body = response.result().bodyAsJsonArray();
          assertEquals(1, body.size());
          testContext.completeNow();
        }));
    }

    @Test
    @DisplayName("Add a new service with a post to /service")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void add_service(Vertx vertx, VertxTestContext testContext) {
        // add a new service
        JsonObject json = new JsonObject().put("name", "kryy").put("url", "http://www.kry.se");
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
                    assertEquals("kryy", service.getString("name"));
                    assertEquals("http://www.kry.se", service.getString("url"));
                    assertEquals(Status.UNKOWN, service.getString("status"));
                    testContext.completeNow();
                }));
    }

}
