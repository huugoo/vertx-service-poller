package se.kry.codetest;

import io.vertx.core.Vertx;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class TestBackgroundPoller {

    @Test
    @DisplayName("Poll a nonexisting url")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void poll_non_existing(Vertx vertx, VertxTestContext testContext) {
        BackgroundPoller backgroundPoller = new BackgroundPoller(vertx);
        String url = "http://wwww.nothing.nothing";

        backgroundPoller.pingService(url).setHandler(result -> {
            if(result.succeeded()) {
                assertEquals(Status.FAILED, result.result());
                testContext.completeNow();
            }
        });
    }

    @Test
    @DisplayName("Poll an existing url")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void poll_existing(Vertx vertx, VertxTestContext testContext) {
        BackgroundPoller backgroundPoller = new BackgroundPoller(vertx);
        String url = "http://www.kry.se";

        backgroundPoller.pingService(url).setHandler(result -> {
            if(result.succeeded()) {
                assertEquals(Status.OK, result.result());
                testContext.completeNow();
            }
        });
    }

}
