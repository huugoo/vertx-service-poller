package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import org.apache.commons.validator.routines.UrlValidator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

  //TODO: Add logger
  //TODO: HashMap is exposed (you shouldn't manipulate it directly)
  private HashMap<String, Service> services = new HashMap<>();
  private DBConnector connector;
  private BackgroundPoller poller;

  @Override
  public void start(Future<Void> startFuture) {
    connector = new DBConnector(vertx);
    poller = new BackgroundPoller(vertx);
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    loadServicesFromDB().setHandler(status -> {
      if (status.succeeded()) {
        vertx.setPeriodic(1000 * 60, timerId -> poller.pollServices(services));
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

  //TODO: separte routes to their own file
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
      List<JsonObject> jsonServices = services
              .entrySet()
              .stream()
              .map(service ->
                      new JsonObject()
                              .put("name", service.getKey())
                              .put("url", service.getValue().url)
                              .put("status", service.getValue().status))
              .collect(Collectors.toList());
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
        services.put(name, new Service(url, Status.UNKOWN));
        // TODO: put the queries in a separate method
        connector.query("INSERT INTO SERVICE VALUES (null, '" + name + "', '" + url + "', datetime('now'))");
        req.response()
                .putHeader("content-type", "text/plain")
                .end("OK");
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
      services.remove(serviceName);
      // TODO: put the queries in a separate method
      connector.query("DELETE from service where name like '" + serviceName + "'");
      req.response()
              .putHeader("content-type", "text/plain")
              .end("OK");
    });
  }

  // TODO: put the queries in a separate file
  private Future<Boolean> loadServicesFromDB() {
    Future<Boolean> status = Future.future();
    Future res = connector.query("Select * from service").setHandler(asyncResult -> {
      if(asyncResult.succeeded()) {
        for (JsonObject row : asyncResult.result().getRows()) {
          String name = row.getString("name");
          String url = row.getString("url");
          services.put(name, new Service(url, Status.UNKOWN));
        }
        status.complete(true);
      }
    });
    return status;
  }

}

