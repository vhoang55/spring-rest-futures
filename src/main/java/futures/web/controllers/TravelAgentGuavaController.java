package futures.web.controllers;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.*;
import futures.model.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static net.javacrumbs.futureconverter.springguava.FutureConverter.toGuavaListenableFuture;

@RestController
public class TravelAgentGuavaController {

    @RequestMapping("/api/travelGuava")
    public DeferredResult<AgentResponse> guavaFutureWithCallback() {
        final long time = System.nanoTime();
        final AgentResponse response = new AgentResponse();
        DeferredResult<AgentResponse> deferredAgentResponse = new DeferredResult<>();

        ListenableFuture<List<AgentResponse>> combinedFutures =
                Futures.successfulAsList(asList(visited(response), recommended(response)));
        Futures.addCallback(combinedFutures,
                new FutureCallback<List<AgentResponse>>() {
                    @Override
                    public void onSuccess(final List<AgentResponse> result) {
                        response.setProcessingTime((System.nanoTime() - time) / 1000000);
                        deferredAgentResponse.setResult(response);
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        deferredAgentResponse.setErrorResult(t);
                    }
                });

        return deferredAgentResponse;
    }

    private ListenableFuture<AgentResponse> visited(final AgentResponse response) {
        ListenableFuture<ResponseEntity<List<Destination>>> visited = visited();

        return Futures.transform(visited, new AsyncFunction<ResponseEntity<List<Destination>>, AgentResponse>() {
            @Override
            public ListenableFuture<AgentResponse> apply(ResponseEntity<List<Destination>> input) throws Exception {
                final SettableFuture<AgentResponse> future = SettableFuture.create();
                future.set(response);
                response.setVisited(input.getBody());
                return future;
            }
        });
    }

    private ListenableFuture<AgentResponse> recommended(final AgentResponse response) {
        // Destinations.
        final ListenableFuture<ResponseEntity<List<Destination>>> recommended = recommmended();
        final ListenableFuture<List<Recommendation>> recommendations = Futures.transform(recommended,
                new AsyncFunction<ResponseEntity<List<Destination>>, List<Recommendation>>() {
                    @Override
                    public ListenableFuture<List<Recommendation>> apply(ResponseEntity<List<Destination>> input) throws Exception {
                        final List<Recommendation> recommendations = Lists.newArrayList(Lists.transform(input.getBody(),
                                new Function<Destination, Recommendation>() {
                                    @Override
                                    public Recommendation apply(final Destination input) {
                                        return new Recommendation(input.getDestination(), null, 0);
                                    }
                                }));
                        return Futures.immediateFuture(recommendations);
                    }
                });

        final ListenableFuture<List<List<Recommendation>>> syncedFuture = Futures.successfulAsList(asList(
                // Add Forecasts to Recommendations.
                forecasts(recommendations),
                // Add Forecasts to Recommendations.
                calculations(recommendations)));

        return Futures.transform(syncedFuture, new AsyncFunction<List<List<Recommendation>>, AgentResponse>() {
            @Override
            public ListenableFuture<AgentResponse> apply(final List<List<Recommendation>> input) throws Exception {
                response.setRecommended(input.get(0));
                return Futures.immediateFuture(response);
            }
        });
    }

    private ListenableFuture<List<Recommendation>> forecasts(final ListenableFuture<List<Recommendation>> recommendations) {
        return Futures.transform(recommendations,
                new AsyncFunction<List<Recommendation>, List<Recommendation>>() {
                    @Override
                    public ListenableFuture<List<Recommendation>> apply(final List<Recommendation> recommendationLists) throws Exception {
                        return Futures.successfulAsList(Lists.transform(recommendationLists,
                                new Function<Recommendation, ListenableFuture<Recommendation>>() {
                                    @Override
                                    public ListenableFuture<Recommendation> apply(final Recommendation r) {
                                        return Futures.transform(
                                                getForcast(r.getDestination()),
                                                new AsyncFunction<ResponseEntity<Forecast>, Recommendation>() {
                                                    @Override
                                                    public ListenableFuture<Recommendation> apply(final ResponseEntity<Forecast> f)
                                                            throws Exception {
                                                        r.setForecast(f.getBody().getForecast());
                                                        return Futures.immediateFuture(r);
                                                    }
                                                });
                                    }
                                }));
                    }
                });
    }

    private ListenableFuture<List<Recommendation>> calculations(final ListenableFuture<List<Recommendation>> recommendations) {
        return Futures.transform(recommendations,
                new AsyncFunction<List<Recommendation>, List<Recommendation>>() {
                    @Override
                    public ListenableFuture<List<Recommendation>> apply(final List<Recommendation> inputs) throws Exception {

                        return Futures.successfulAsList(Lists.transform(inputs,
                                new Function<Recommendation, ListenableFuture<Recommendation>>() {
                                    @Override
                                    public ListenableFuture<Recommendation> apply(final Recommendation r) {
                                        return Futures.transform(
                                                price("moon", r.getDestination()),
                                                new AsyncFunction<ResponseEntity<Calculation>, Recommendation>() {
                                                    @Override
                                                    public ListenableFuture<Recommendation> apply(final ResponseEntity<Calculation> f)
                                                            throws Exception {
                                                        r.setPrice(f.getBody().getPrice());
                                                        return Futures.immediateFuture(r);
                                                    }
                                                });
                                    }
                                }));
                    }
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
        headers.add("Rx-User", "Guava");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private AsyncRestTemplate asyncRestTemplate() {
        return new AsyncRestTemplate();
    }



}
