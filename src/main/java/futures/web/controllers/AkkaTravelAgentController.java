package futures.web.controllers;


import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import akka.japi.Function;
import akka.pattern.Patterns;
import akka.routing.RoundRobinPool;
import akka.util.Timeout;
import futures.SpringAppContext;
import futures.actors.PriceCalculationActor;
import futures.actors.RecommenderActor;
import futures.actors.WeatherForecastActor;
import futures.messages.*;
import futures.model.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import scala.Tuple2;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
public class AkkaTravelAgentController {

    final ActorRef recommenderActor = SpringAppContext.actorSystem().actorOf(Props.create(RecommenderActor.class).withRouter(new RoundRobinPool(5)));

    @RequestMapping("/api/akkaActorTravelAgent")
    public DeferredResult<AgentResponse> processRequest() {
        final DeferredResult<AgentResponse> deferredResult = new DeferredResult<>();
        final long time = System.nanoTime();

        final Future<Object> visitedRequest = Patterns.ask(recommenderActor, new RequestVisit(),  new Timeout(Duration.create(5, TimeUnit.SECONDS)));
        final Future<Object> recommendations = Patterns.ask(recommenderActor, new RecommendationRequest(),  new Timeout(Duration.create(5, TimeUnit.SECONDS)));

        final Future<List<Destination>> destinationsFutures = toDestinations(recommendations);
        final Future<List<Destination>> visitedFutures = toVisits(visitedRequest);

        final Future<Iterable<Recommendation>> recommendationsFuture = toTravelRecommendationWithInfo(destinationsFutures);
        final Future<AgentResponse> agentResponseFuture = toAgentResponse(visitedFutures, recommendationsFuture);

        agentResponseFuture.onComplete(new OnComplete<AgentResponse>() {
            @Override
            public void onComplete(Throwable failure, AgentResponse response) throws Throwable {
                if (response != null) {
                    response.setProcessingTime((System.nanoTime() - time) / 1000000);
                    deferredResult.setResult(response);

                } else {
                    deferredResult.setErrorResult(failure);
                }
            }
        }, SpringAppContext.actorSystem().dispatcher());

        return deferredResult;

    }

    private Future<List<Destination>> toDestinations(Future<Object> recommendations) {
        return recommendations.map(new Mapper<Object, List<Destination>>() {
            public List<Destination> apply(Object result) {
                RecommendationResponse response = (RecommendationResponse) result;
                return response.getRecommendDestination();
            }
        }, SpringAppContext.actorSystem().dispatcher());
    }

    private Future<List<Destination>> toVisits(Future<Object> visits) {
        return visits.map(new Mapper<Object, List<Destination>>() {
            public List<Destination> apply(Object result) {
                ResponseVisit visits = (ResponseVisit) result;
                return visits.getVisitedDestination();
            }
        }, SpringAppContext.actorSystem().dispatcher());
    }

    private Future<AgentResponse> toAgentResponse(Future<List<Destination>> visitedFutures, Future<Iterable<Recommendation>> reccomendationsFuture) {
        return visitedFutures.zip(reccomendationsFuture).map(new Mapper<Tuple2<List<Destination>, Iterable<Recommendation>>, AgentResponse>() {
            @Override
            public AgentResponse apply(Tuple2<List<Destination>, Iterable<Recommendation>> tuple) {
                return new AgentResponse(tuple._1(), toList(tuple._2()));
            }
        }, SpringAppContext.actorSystem().dispatcher());
    }

    private Future<Iterable<Recommendation>> toTravelRecommendationWithInfo(Future<List<Destination>> destinationsFutures) {
        return destinationsFutures.flatMap(new Mapper<List<Destination>, Future<Iterable<Recommendation>>>() {
            @Override
            public Future<Iterable<Recommendation>> apply(List<Destination> destinations) {
                return Futures.traverse(destinations,
                        new Function<Destination, Future<Recommendation>>() {
                            @Override
                            public Future<Recommendation> apply(Destination d) throws Exception {
                                return getDestinationWithReccomendation(d);
                            }
                        }, SpringAppContext.actorSystem().dispatcher());
            }
        }, SpringAppContext.actorSystem().dispatcher());
    }

    private Future<Recommendation> getDestinationWithReccomendation(Destination destination) {

        ActorRef pricingActor = SpringAppContext.actorSystem().actorOf(Props.create(PriceCalculationActor.class));
        ActorRef foreCastActor = SpringAppContext.actorSystem().actorOf(Props.create(WeatherForecastActor.class));

        final Future<Calculation> calculationFuture = toPrice(Patterns.ask(pricingActor, new PriceRequest("moon", destination.getDestination()),  new Timeout(Duration.create(5, TimeUnit.SECONDS))));
        final Future<Forecast> forecastFuture = toForecast(Patterns.ask(foreCastActor, new WeatherRequest(destination.getDestination()), new Timeout(Duration.create(5, TimeUnit.SECONDS))));

        return calculationFuture.zip(forecastFuture).map(new Mapper<Tuple2<Calculation, Forecast>, Recommendation>() {
            @Override
            public Recommendation apply(Tuple2<Calculation, Forecast> tuple) {
                return new Recommendation(destination.getDestination(), tuple._2().getForecast(), tuple._1().getPrice());
            }
        }, SpringAppContext.actorSystem().dispatcher());
    }

    private Future<Calculation> toPrice(Future<Object> futureResponse) {
        return futureResponse.map(new Mapper<Object, Calculation>() {
            public Calculation apply(Object result) {
                PriceResponse price = (PriceResponse) result;
                return price.getPrice();
            }
        }, SpringAppContext.actorSystem().dispatcher());
    }

    private Future<Forecast> toForecast(Future<Object> futureResponse) {
        return futureResponse.map(new Mapper<Object, Forecast>() {
            public Forecast apply(Object result) {
                WeatherResponse weather = (WeatherResponse) result;
                return weather.getForecast();
            }
        }, SpringAppContext.actorSystem().dispatcher());
    }

    public static <T> List<T> toList(final Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false)
                .collect(Collectors.toList());
    }


}
