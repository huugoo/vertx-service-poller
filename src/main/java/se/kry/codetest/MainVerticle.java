package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import org.apache.commons.validator.routines.UrlValidator;

import java.util.List;

public class MainVerticle extends AbstractVerticle {

  //TODO: Add logger
  private DBConnector connector;
  private BackgroundPoller poller;
  private ServiceRepository serviceRepo;

  @Override
  public void start(Future<Void> startFuture) {
    connector = new DBConnector(vertx);
    poller = new BackgroundPoller(vertx);
    serviceRepo = new ServiceRepository(connector);
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    serviceRepo.loadServicesFromDB().setHandler(status -> {
      if (status.succeeded()) {
        vertx.setPeriodic(1000 * 60, timerId -> poller.pollServices(serviceRepo));
        setRoutes(router);
        vertx
                .createHttpServer()
                .requestHandler(router)
                .listen(8080, result -> {
                  if (result.succeeded()) {
                    System.out.println("KRY code test service started");
                    startFuture.complete();
                  } else {
                    startFuture.fail(result.cause());
                  }
                });
      }
    });
  }

  //TODO: separate routes to their own file
  private void setRoutes(Router router) {
    staticRoute(router);
    getServicesRoute(router);
    postServiceRoute(router);
    deleteServiceRoute(router);
  }

  private void staticRoute(Router router) {
    router.route("/*").handler(StaticHandler.create());
  }

  private void getServicesRoute(Router router) {
    router.get("/service").handler(req -> {
      List<JsonObject> jsonServices = serviceRepo.getServicesList();
      req.response()
              .putHeader("content-type", "application/json")
              .end(new JsonArray(jsonServices).encode());
    });
  }

  private void postServiceRoute(Router router) {
    router.post("/service").handler(req -> {
      JsonObject jsonBody = req.getBodyAsJson();
      String name = jsonBody.getString("name");
      String url = jsonBody.getString("url");

      if (name == null || name.isEmpty() || !validateUrl(url)) {
        req.response()
                .setStatusCode(400)
                .putHeader("content-type", "text/plain")
                .end("Invalid url: " + url);
      } else {
        serviceRepo.addService(name, url).setHandler(res -> {
          if (res.succeeded()) {
            req.response()
                    .putHeader("content-type", "text/plain")
                    .end("OK");
          } else {
            req.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "text/plain")
                    .end("Could not add Service");
          }
        });
      }
    });
  }

  private boolean validateUrl(String url) {
    String[] schemes = {"http","https"};
    UrlValidator urlValidator = new UrlValidator(schemes);
    return urlValidator.isValid(url);
  }

  private void deleteServiceRoute(Router router) {
    router.delete("/service/:serviceName").handler(req -> {
      String serviceName =  req.pathParam("serviceName");
      serviceRepo.deleteService(serviceName);
      req.response()
              .putHeader("content-type", "text/plain")
              .end("OK");
    });
  }

}

