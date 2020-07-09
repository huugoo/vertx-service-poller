package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

  private HashMap<String, Service> services = new HashMap<>();
  //TODO use this
  private DBConnector connector;
  private BackgroundPoller poller = new BackgroundPoller();

  @Override
  public void start(Future<Void> startFuture) {
    connector = new DBConnector(vertx);
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    // services.put("kry", new Service("https://www.kry.se", Status.UNKOWN));
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

  private void setRoutes(Router router){
    router.route("/*").handler(StaticHandler.create());
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
    router.post("/service").handler(req -> {
      JsonObject jsonBody = req.getBodyAsJson();
      String name = jsonBody.getString("name");
      String url = jsonBody.getString("url");
      services.put(name, new Service(url, Status.UNKOWN));
      connector.query("INSERT INTO SERVICE VALUES (null, '" + name + "', '" + url + "', datetime('now'))");
      req.response()
          .putHeader("content-type", "text/plain")
          .end("OK");
    });

    router.delete("/service/:serviceName").handler(req -> {
      String serviceName =  req.pathParam("serviceName");
      services.remove(serviceName);
      connector.query("DELETE from service where name like '" + serviceName + "'");
      req.response()
              .putHeader("content-type", "text/plain")
              .end("OK");
    });
  }


  private Future<Boolean> loadServicesFromDB() {
    Future<Boolean> status = Future.future();
    System.out.println("loadServices");
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



