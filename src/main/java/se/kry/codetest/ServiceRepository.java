package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

// TODO: consider adding a ServiceRepository interface
public class ServiceRepository {

    private final DBConnector connector;
    private final HashMap<String, Service> services = new HashMap<>();

    public ServiceRepository(DBConnector connector) {
        this.connector = connector;
    }

    // TODO: use java.time instead
    public Future<Void> addService(String name, String url) {
        Future<Void> addFuture = Future.future();
        JsonArray params = new JsonArray().add(name).add(url);
        connector.query("INSERT INTO SERVICE VALUES (null, ?, ?, datetime('now'))", params).setHandler(asyncResult -> {
            if(asyncResult.succeeded()) {
                services.put(name, new Service(url, Status.UNKNOWN));
                addFuture.complete();
            } else {
                addFuture.fail("Could not add Service");
            }
        });

        return addFuture;
    }

    public void deleteService(String serviceName) {
        JsonArray params = new JsonArray().add(serviceName);
        connector.query("DELETE from service where name = ?", params);
        services.remove(serviceName);
    }

    public Status getStatus(String serviceName) {
        return services.get(serviceName).status;
    }

    // TODO: consider changing to List<String> getServicesNames(), and setStatus
    public HashMap<String, Service> getServicesMap() {
        // return a shallow copy
        return new HashMap<>(services);
    }

    public List<JsonObject> getServicesList() {
        return services.entrySet().stream().map(service ->
                        new JsonObject()
                                .put("name", service.getKey())
                                .put("url", service.getValue().url)
                                .put("status", service.getValue().status))
                .collect(Collectors.toList());
    }

    public Future<Void> loadServicesFromDB() {
        Future<Void> status = Future.future();
        connector.query("Select * from service").setHandler(asyncResult -> {
            if(asyncResult.succeeded()) {
                for (JsonObject row : asyncResult.result().getRows()) {
                    String name = row.getString("name");
                    String url = row.getString("url");
                    services.put(name, new Service(url, Status.UNKNOWN));
                }
                status.complete();
            }
        });
        return status;
    }

}
