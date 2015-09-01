package futures.actors;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import futures.AkkaActors;
import futures.messages.WeatherRequest;
import futures.messages.WeatherResponse;
import futures.model.Forecast;
import futures.web.controllers.HeaderRequestInterceptor;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestTemplate;

import javax.inject.Named;

import static java.util.Arrays.asList;


@Named(AkkaActors.FORECAST_ACTOR)
@Scope("prototype")
public class WeatherForecastActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(context().system(), this);
    private static final String url = "http://localhost:8082/remote/forecast/%s";

    public WeatherForecastActor() {
        receive(
                ReceiveBuilder
                        .match(WeatherRequest.class, fc -> {
                            RestTemplate restTemplate = new RestTemplate();
                            restTemplate.setInterceptors(asList(new HeaderRequestInterceptor("User", "Akka")));
                            Forecast forecast = restTemplate.getForObject(String.format(url, fc.getDestination()), Forecast.class);
                            sender().tell(new WeatherResponse(forecast), self());
                        })
                        .matchAny(o -> log.info("WeatherForecastActor: received unknown message ", o))
                        .build()
        );

    }
}

