package se.kry.codetest;

//TODO: rename to ServiceStatus
enum Status
{
    UNKNOWN, OK, FAILED;
}

// TODO: consider adding getters and setters
public class Service {
    public String url;
    public Status status;

    Service(String url, Status status) {
        this.url = url;
        this.status = status;
    }

}
