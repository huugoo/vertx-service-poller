package se.kry.codetest;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class TestBackgroundPoller {

    @Test
    @DisplayName("Poll a nonexistent url with pollService(service)")
    @Timeout(value = 20, timeUnit = TimeUnit.SECONDS)
    void poll_non_existing(Vertx vertx, VertxTestContext testContext) {
        BackgroundPoller backgroundPoller = new BackgroundPoller(vertx);

        String url = "http://www.nothing.nothing";
        Service service = new Service(url, Status.UNKNOWN);

        backgroundPoller.pollService(service).setHandler(result -> {
            if(result.succeeded()) {
                assertEquals(Status.FAILED, result.result());
                testContext.completeNow();
            }
        });
    }

    @Test
    @DisplayName("Poll an existing url with pollService(service)")
    @Timeout(value = 20, timeUnit = TimeUnit.SECONDS)
    void poll_existing(Vertx vertx, VertxTestContext testContext) {
        BackgroundPoller backgroundPoller = new BackgroundPoller(vertx);

        String url = "http://www.kry.se";
        Service service = new Service(url, Status.UNKNOWN);

        backgroundPoller.pollService(service).setHandler(result -> {
            if(result.succeeded()) {
                assertEquals(Status.OK, result.result());
                testContext.completeNow();
            }
        });
    }

    @Test
    @DisplayName("Poll two existing and two nonexistent urls with pollServices(repo)")
    @Timeout(value = 20, timeUnit = TimeUnit.SECONDS)
    void poll_3_existing2(Vertx vertx, VertxTestContext testContext) {
        BackgroundPoller backgroundPoller = new BackgroundPoller(vertx);
        ServiceRepository repo = new ServiceRepository(new DBConnector(vertx));

        String url1 = "http://www.kry.se";
        String url2 = "http://www.google.com";
        String url3 = "aNonexistentURL";
        String url4 = "aNonexistentURL";
        String serviceName1 = "test_kry";
        String serviceName2 = "test_google";
        String serviceName3 = "test_nonexistent1";
        String serviceName4 = "test_nonexistent2";
        Future<Void> addFuture1 = repo.addService(serviceName1, url1);
        Future<Void> addFuture2 = repo.addService(serviceName2, url2);
        Future<Void> addFuture3 = repo.addService(serviceName3, url3);
        Future<Void> addFuture4 = repo.addService(serviceName4, url4);

        List<Future> addFutures = Arrays.asList(addFuture1, addFuture2, addFuture3, addFuture4);

        CompositeFuture.all(addFutures).setHandler(res -> {
            if(res.succeeded()) {
                List<Future> futureStatuses = backgroundPoller.pollServices(repo);

                CompositeFuture.all(futureStatuses).setHandler(res2 -> {
                    if(res2.succeeded()) {
                        assertEquals(Status.OK, repo.getStatus(serviceName1));
                        assertEquals(Status.OK, repo.getStatus(serviceName2));
                        assertEquals(Status.FAILED, repo.getStatus(serviceName3));
                        assertEquals(Status.FAILED, repo.getStatus(serviceName4));
                        testContext.completeNow();
                    }
                });
            }
        });
    }

}
