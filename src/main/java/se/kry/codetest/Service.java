package se.kry.codetest;

enum Status
{
    UNKOWN, OK, FAILED;
}

public class Service {
    public String url;
    public Status status;

    Service(String url, Status status){
        this.url = url;
        this.status = status;
    }

}
