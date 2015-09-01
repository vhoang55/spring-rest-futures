package futures.messages;


import futures.model.Forecast;

public class WeatherResponse {
    final Forecast forecast;

    public WeatherResponse(Forecast forecast){
        this.forecast = forecast;
    }

    public Forecast getForecast() {
        return this.forecast;
    }

}
