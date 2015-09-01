package futures.web.controllers;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.*;
import futures.model.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;

import static java.util.Arrays.asList;
import static net.javacrumbs.futureconverter.springguava.FutureConverter.toGuavaListenableFuture;

@RestController
public class GuavaWithLamdaController {

    @RequestMapping("/api/travelGuavaLamda")
    public DeferredResult<AgentResponse> futuresWithLambda() {
        final long time = System.nanoTime();
        final AgentResponse response = new AgentResponse();
        DeferredResult<AgentResponse> deffered = new DeferredResult<>();

        Futures.addCallback(Futures.successfulAsList(asList(visited(response), recommended(response))),
                new FutureCallback<List<AgentResponse>>() {
                    @Override
                    public void onSuccess(final List<AgentResponse> result) {
                        response.setProcessingTime((System.nanoTime() - time) / 1000000);
                        deffered.setResult(response);
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        deffered.setErrorResult(t);
                    }
                });

        return deffered;
    }

    private ListenableFuture<AgentResponse> visited(final AgentResponse response) {
        ListenableFuture<ResponseEntity<List<Destination>>> visited = visited();

        return Futures.transform(visited, (ResponseEntity<List<Destination>> destinations) -> {
            final SettableFuture<AgentResponse> future = SettableFuture.create();
            future.set(response);
            response.setVisited(destinations.getBody());
            return future;
        });
    }

    private ListenableFuture<AgentResponse> recommended(final AgentResponse response) {
        // Destinations.
        final ListenableFuture<ResponseEntity<List<Destination>>> recommended = recommmended();
        final ListenableFuture<List<Recommendation>> recommendations = Futures.transform(recommended,
                (ResponseEntity<List<Destination>> destinations) -> {
                    final List<Recommendation> recommendations1 = Lists.newArrayList(Lists.transform(destinations.getBody(),
                            destination -> new Recommendation(destination.getDestination(), null, 0)));
                    return Futures.immediateFuture(recommendations1);
                });

        final ListenableFuture<List<List<Recommendation>>> syncedFuture = Futures.successfulAsList(asList(
                // Add Forecasts to Recommendations.
                forecasts(recommendations),
                // Add Forecasts to Recommendations.
                calculations(recommendations)));

        return Futures.transform(syncedFuture, (List<List<Recommendation>> recomendation) -> {
            response.setRecommended(recomendation.get(0));
            return Futures.immediateFuture(response);
        });
    }

    private ListenableFuture<List<Recommendation>> forecasts(final ListenableFuture<List<Recommendation>> recommendations) {
        return Futures.transform(recommendations,
                new AsyncFunction<List<Recommendation>, List<Recommendation>>() {
                    @Override
                    public ListenableFuture<List<Recommendation>> apply(final List<Recommendation> input) throws Exception {
                        return Futures.successfulAsList(Lists.transform(input,
                                recommendation -> Futures.transform(getForcast(recommendation.getDestination()),
                                        (ResponseEntity<Forecast> forcast) -> {
                                            recommendation.setForecast(forcast.getBody().getForecast());
                                            return Futures.immediateFuture(recommendation);
                                        })));
                    }
                });
    }

    private ListenableFuture<List<Recommendation>> calculations(final ListenableFuture<List<Recommendation>> recommendations) {
        return Futures.transform(recommendations,
                (List<Recommendation> destinations) -> {
                    return Futures.successfulAsList(Lists.transform(destinations,
                            recommendation -> Futures.transform(
                                    price("moon", recommendation.getDestination()),
                                    (ResponseEntity<Calculation> calculation) -> {
                                        recommendation.setPrice(calculation.getBody().getPrice());
                                        return Futures.immediateFuture(recommendation);
                                    })));
                });
    }


    private ListenableFuture<ResponseEntity<List<Destination>>> getResponseEntityListenableFuture(String url) {
        ParameterizedTypeReference<List<Destination>> responseType = new ParameterizedTypeReference<List<Destination>>() {};
        HttpEntity<String> requestEntity = new HttpEntity<>("params", getHttpHeaders());
        org.springframework.util.concurrent.ListenableFuture<ResponseEntity<List<Destination>>> result = asyncRestTemplate().exchange(url, HttpMethod.GET, requestEntity, responseType);
        return toGuavaListenableFuture(result);
    }


    private ListenableFuture<ResponseEntity<List<Destination>>> visited() {
        String url = "http://localhost:8082/remote/destination/visited";
        ListenableFuture<ResponseEntity<List<Destination>>> visited = getResponseEntityListenableFuture(url);
        return visited;
    }

    private ListenableFuture<ResponseEntity<List<Destination>>> recommmended() {
        String url = "http://localhost:8082/remote/destination/recommended";
        ListenableFuture<ResponseEntity<List<Destination>>> visited = getResponseEntityListenableFuture(url);
        return visited;
    }

    private ListenableFuture<ResponseEntity<Forecast>> getForcast(String destination) {
        String url = "http://localhost:8083/remote/forecast/%s";
        ParameterizedTypeReference<Forecast> responseType = new ParameterizedTypeReference<Forecast>() {};
        HttpEntity<String> requestEntity = new HttpEntity<>("params", getHttpHeaders());
        org.springframework.util.concurrent.ListenableFuture<ResponseEntity<Forecast>> result = asyncRestTemplate().exchange(String.format(url, destination), HttpMethod.GET, requestEntity, responseType);
        return toGuavaListenableFuture(result);
    }

    private ListenableFuture<ResponseEntity<Calculation>> price(String from, String to) {
        String url = "http://localhost:8081/remote/calculation/from/%s/to/%s";
        ParameterizedTypeReference<Calculation> responseType = new ParameterizedTypeReference<Calculation>() {};
        HttpEntity<String> requestEntity = new HttpEntity<>("params", getHttpHeaders());
        org.springframework.util.concurrent.ListenableFuture<ResponseEntity<Calculation>> result = asyncRestTemplate().exchange(String.format(url, from, to), HttpMethod.GET, requestEntity, responseType);
        return toGuavaListenableFuture(result);
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("User", "Guava");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private AsyncRestTemplate asyncRestTemplate() {
        return new AsyncRestTemplate();
    }


}
