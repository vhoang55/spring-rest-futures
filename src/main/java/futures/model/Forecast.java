package futures.model;


public class Forecast {

    private String forecast;
    private String destination;

    public Forecast() {
    }

    public Forecast(final String destination, final String forecast) {
        this.destination = destination;
        this.forecast = forecast;
    }

    public String getForecast() {
        return forecast;
    }

    public void setForecast(final String forecast) {
        this.forecast = forecast;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(final String destination) {
        this.destination = destination;
    }
}
