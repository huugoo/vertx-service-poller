package se.kry.codetest.migrate;

import io.vertx.core.Vertx;
import se.kry.codetest.DBConnector;


// TODO: change to a method that returns a Future when the tables are created (for testing)
public class DBMigration {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    DBConnector connector = new DBConnector(vertx);
    connector.query("CREATE TABLE IF NOT EXISTS service (ID integer primary key autoincrement, " +
            "name VARCHAR(128) NOT NULL, url VARCHAR(128) NOT NULL," +
            "createdAt DATETIME DEFAULT CURRENT_TIMESTAMP)").setHandler(done -> {
      if(done.succeeded()){
        System.out.println("completed db migrations");
      } else {
        done.cause().printStackTrace();
      }
      vertx.close(shutdown -> {
        System.exit(0);
      });
    });
  }
}
