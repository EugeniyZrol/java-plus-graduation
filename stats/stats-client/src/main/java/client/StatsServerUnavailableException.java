package client;

public class StatsServerUnavailableException extends RuntimeException {
    public StatsServerUnavailableException(String message) {
        super(message);
    }
}
