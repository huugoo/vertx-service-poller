package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class BackgroundPoller {
  private WebClient webClient;

  BackgroundPoller(Vertx vertx) {
    this.webClient = WebClient.create(vertx);
  }

  //public List<Future<Status>>  pollServices(Map<String, Service> services) {
  public void pollServices(Map<String, Service> services) {

//    List<Future<Status>> statuses = services
//            .entrySet()
//            .stream()
//            .map(service -> pingService(service.getValue().url).setHandler(asyncResult -> {
//              if (asyncResult.succeeded()) {
//                Service serv = service.getValue();
//                serv.status = asyncResult.result();
//                services.put(service.getKey(), serv);
//              }
//            })).collect(Collectors.toList());
//
//    return statuses;
    services.forEach((name, Service) -> pingService(Service.url).setHandler(asyncResult -> {
      if (asyncResult.succeeded()) {
        Service.status = asyncResult.result();
        services.put(name, Service);
      }
    }));

  }

//  private Future<Status> pingService(String serviceName, Service service) {
//    Future<Status> futureStatus = Future.future();
//    try {
//      webClient.getAbs(service.url).send(response -> {
//        if (response.succeeded()) {
//          futureStatus.complete(service.status = (200 == response.result().statusCode() ? Status.OK : Status.FAILED));
//        } else {
//          futureStatus.complete(service.status = Status.FAILED);
//        }
//      });
//    } catch (Exception e) {
//      futureStatus.complete(Status.FAILED);
//    }
//    return futureStatus;
//  }

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
