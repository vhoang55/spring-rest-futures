package futures.messages;


public class WeatherRequest {
    private String destination;
    public WeatherRequest(String destination) {
        this.destination = destination;
    }

    public String getDestination() {
        return this.destination;
    }
}
