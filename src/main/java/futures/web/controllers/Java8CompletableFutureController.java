package futures.web.controllers;

import futures.model.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
public class Java8CompletableFutureController {

    @RequestMapping("/api/travelJava8")
    public DeferredResult<AgentResponse> futureComposing() {
        DeferredResult deferredResult = new DeferredResult();
        final long time = System.nanoTime();

        CompletableFuture.completedFuture(new AgentResponse())
                .thenCombine(visited(), AgentResponse::visited)
                .thenCombine(getRecommendations(), AgentResponse::recommended)
                .whenCompleteAsync((response, throwable) -> {
                    // ignore error for now
                    response.setProcessingTime((System.nanoTime() - time) / 1000000);
                    deferredResult.setResult(response);
                });

        return  deferredResult;
    }

    private CompletableFuture<List<Recommendation>> getRecommendations() {
        CompletableFuture<List<Recommendation>> futureRecommendation = recommmended().thenCompose(destinations -> {
            List<CompletableFuture<Recommendation>> recommendations = destinations.stream().map(destination -> {
                CompletableFuture<Forecast> forecast = getForcast(destination.getDestination());
                CompletableFuture<Calculation> price = price("moon", destination.getDestination());
                return CompletableFuture
                        .completedFuture(new Recommendation(destination))
                        .thenCombine(forecast, Recommendation::forecast)
                        .thenCombine(price, Recommendation::calculation);
            }).collect(Collectors.toList());

            return sequence(recommendations);
        });
        return futureRecommendation;
    }


    private static <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
        CompletableFuture<Void> allDoneFuture =
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
        return allDoneFuture.thenApply(v ->
                        futures.stream().
                                map(future -> future.join()).
                                collect(Collectors.<T>toList())
        );
    }


    private CompletableFuture<List<Destination>> getCompletetableDestination(String url) {
        final CompletableFuture<List<Destination>> promise = new CompletableFuture<>();
        ParameterizedTypeReference<List<Destination>> responseType = new ParameterizedTypeReference<List<Destination>>() {};
        HttpEntity<String> requestEntity = new HttpEntity<>("params", getHttpHeaders());
        org.springframework.util.concurrent.ListenableFuture<ResponseEntity<List<Destination>>> result = asyncRestTemplate().exchange(url, HttpMethod.GET, requestEntity, responseType);
        result.addCallback(new ListenableFutureCallback<ResponseEntity<List<Destination>>>() {
            @Override
            public void onFailure(Throwable ex) {
                 promise.completeExceptionally(ex);
            }

            @Override
            public void onSuccess(ResponseEntity<List<Destination>> result) {
                promise.complete(result.getBody());
            }
        });
        return promise;
    }


    private CompletableFuture<List<Destination>> visited() {
        String url = "http://localhost:8082/remote/destination/visited";
        CompletableFuture<List<Destination>> visited = getCompletetableDestination(url);
        return visited;
    }

    private CompletableFuture<List<Destination>> recommmended() {
        String url = "http://localhost:8082/remote/destination/recommended";
        CompletableFuture<List<Destination>> visited = getCompletetableDestination(url);
        return visited;
    }

    private CompletableFuture<Forecast> getForcast(String destination) {
        final CompletableFuture<Forecast> promise = new CompletableFuture<>();
        String url = "http://localhost:8083/remote/forecast/%s";
        ParameterizedTypeReference<Forecast> responseType = new ParameterizedTypeReference<Forecast>() {};
        HttpEntity<String> requestEntity = new HttpEntity<>("params", getHttpHeaders());
        ListenableFuture<ResponseEntity<Forecast>> result = asyncRestTemplate().exchange(String.format(url, destination), HttpMethod.GET, requestEntity, responseType);
        result.addCallback(new ListenableFutureCallback<ResponseEntity<Forecast>>() {
            @Override
            public void onFailure(Throwable ex) {
                promise.completeExceptionally(ex);
            }

            @Override
            public void onSuccess(ResponseEntity<Forecast> result) {
                promise.complete(result.getBody());
            }
        });
        return promise;
    }

    private CompletableFuture<Calculation> price(String from, String to) {
        final CompletableFuture<Calculation> promise = new CompletableFuture<>();
        String url = "http://localhost:8081/remote/calculation/from/%s/to/%s";
        ParameterizedTypeReference<Calculation> responseType = new ParameterizedTypeReference<Calculation>() {};
        HttpEntity<String> requestEntity = new HttpEntity<>("params", getHttpHeaders());
        org.springframework.util.concurrent.ListenableFuture<ResponseEntity<Calculation>> result = asyncRestTemplate().exchange(String.format(url, from, to), HttpMethod.GET, requestEntity, responseType);
        result.addCallback(new ListenableFutureCallback<ResponseEntity<Calculation>>() {
            @Override
            public void onFailure(Throwable ex) {
                promise.completeExceptionally(ex);
            }

            @Override
            public void onSuccess(ResponseEntity<Calculation> result) {
                promise.complete(result.getBody());
            }
        });
        return promise;
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("User", "Java8");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private AsyncRestTemplate asyncRestTemplate() {
        return new AsyncRestTemplate();
    }
}
