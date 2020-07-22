package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BackgroundPoller {
  private WebClient webClient;

  BackgroundPoller(Vertx vertx) {
    this.webClient = WebClient.create(vertx);
  }

  // TODO: Avoid raw type
  public List<Future> pollServices(ServiceRepository repo) {
    List<Future> futureStatuses = new ArrayList<>();
    Map<String, Service> serviceMap = repo.getServicesMap();

    for (Map.Entry<String, Service> entry : serviceMap.entrySet() ) {
      Future<Void> newFuture = Future.future();
      futureStatuses.add(newFuture);
      pollService(entry.getValue()).setHandler(result -> {
        if (result.succeeded()) {
          newFuture.complete();
        }
      });
    }
    return futureStatuses;
  }

  public Future<Status> pollService(Service service) {
    Future<Status> futureStatus = Future.future();
    try {
      webClient.getAbs(service.url).send(response -> {
        if (response.succeeded()) {
          Status newStatus = (200 == response.result().statusCode() ? Status.OK : Status.FAILED);
          service.status = newStatus;
          futureStatus.complete(newStatus);
        } else {
          service.status = Status.FAILED;
          futureStatus.complete(Status.FAILED);
        }
      });
    } catch (Exception e) {
      service.status = Status.FAILED;
      futureStatus.complete(Status.FAILED);
    }
    return futureStatus;
  }

}
