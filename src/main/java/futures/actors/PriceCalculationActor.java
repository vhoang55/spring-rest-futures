package futures.actors;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;

import futures.AkkaActors;
import futures.messages.PriceRequest;
import futures.messages.PriceResponse;
import futures.model.Calculation;
import futures.web.controllers.HeaderRequestInterceptor;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestTemplate;

import javax.inject.Named;

import static java.util.Arrays.asList;

@Named(AkkaActors.PRICING_ACTOR)
@Scope("prototype")
public class PriceCalculationActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(context().system(), this);
    private static final String url = "http://localhost:8081/remote/calculation/from/%s/to/%s";


    public PriceCalculationActor() {
        receive(
                ReceiveBuilder
                        .match(PriceRequest.class, pr -> {
                            RestTemplate restTemplate = new RestTemplate();
                            restTemplate.setInterceptors(asList(new HeaderRequestInterceptor("User", "Akka")));
                            Calculation price = restTemplate.getForObject(String.format(url, pr.getFrom(), pr.getTo()), Calculation.class);
                            sender().tell(new PriceResponse(price), self());
                        })
                        .matchAny(o -> log.info("PriceCalculationActor: received unknown message", o))
                        .build()
        );

    }
}
