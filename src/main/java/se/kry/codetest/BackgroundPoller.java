package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import java.util.Map;

public class BackgroundPoller {
  private WebClient webClient;

  BackgroundPoller(Vertx vertx) {
    this.webClient = WebClient.create(vertx);
  }

  public void pollServices(Map<String, Service> services) {
    services.forEach((name, Service) -> pingService(Service.url).setHandler(asyncResult -> {
      if (asyncResult.succeeded()) {
        Service.status = asyncResult.result();
        services.put(name, Service);
      }
    }));
  }

  public Future<Status> pingService(String url) {
    Future<Status> status = Future.future();
    try {
      webClient.getAbs(url).send(response -> {
        if (response.succeeded()) {
          status.complete(200 == response.result().statusCode() ? Status.OK : Status.FAILED);
        } else {
          status.complete(Status.FAILED);
        }
      });
    } catch (Exception e) {
      status.complete(Status.FAILED);
    }
    return status;
  }
}
