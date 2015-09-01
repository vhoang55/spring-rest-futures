package futures.actors;


import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import futures.AkkaActors;
import futures.messages.RecommendationRequest;
import futures.messages.RecommendationResponse;
import futures.messages.RequestVisit;
import futures.messages.ResponseVisit;
import futures.model.Destination;
import futures.web.controllers.HeaderRequestInterceptor;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestTemplate;

import javax.inject.Named;
import java.util.List;

import static java.util.Arrays.asList;

@Named(AkkaActors.RECOMMENDING_ACTOR)
@Scope("prototype")
public class RecommenderActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(context().system(), this);
    private static final String visitedUrl = "http://localhost:8082/remote/destination/visited";
    private static final String recommendedUrl = "http://localhost:8082/remote/destination/recommended";
    private RestTemplate restTemplate = new RestTemplate();

    public RecommenderActor() {
        receive(
                ReceiveBuilder
                        .match(RequestVisit.class, rv -> {
                            restTemplate.setInterceptors(asList(new HeaderRequestInterceptor("User", "Akka")));
                            List<Destination> visited = asList(restTemplate.getForObject(visitedUrl, Destination[].class));
                            sender().tell(new ResponseVisit(visited), self());
                        })
                        .match(RecommendationRequest.class, rr -> {
                            List<Destination> recommended = asList(restTemplate.getForObject(recommendedUrl, Destination[].class));
                            sender().tell(new RecommendationResponse(recommended), self());
                        })
                        .matchAny(o -> log.info("received unknown message"))
                        .build()
        );

    }
}
