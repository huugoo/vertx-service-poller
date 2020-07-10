package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceRepository {

    private final DBConnector connector;
    private final HashMap<String, Service> services = new HashMap<>();

    public ServiceRepository(DBConnector connector) {
        this.connector = connector;
    }

    public void addService(String name, String url) {
        connector.query("INSERT INTO SERVICE VALUES (null, '" + name + "', '" + url + "', datetime('now'))");
        services.put(name, new Service(url, Status.UNKOWN));
    }

    public void deleteService(String serviceName) {
        connector.query("DELETE from service where name like '" + serviceName + "'");
        services.remove(serviceName);
    }

    //TODO: dont expose services
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

    public Future<Boolean> loadServicesFromDB() {
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
