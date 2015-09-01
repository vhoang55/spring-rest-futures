package futures.web.controllers;

import futures.model.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.context.request.async.DeferredResult;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.Collections;
import java.util.List;

import static net.javacrumbs.futureconverter.springrx.FutureConverter.toObservable;


@RestController
public class TravelAgentRxJavaController {

    @RequestMapping("/api/rxJavaTravelAgent")
    public DeferredResult<AgentResponse> observable() {
        final long time = System.nanoTime();
        DeferredResult<AgentResponse> deffered = new DeferredResult<>();
        Observable.just(new AgentResponse())
                .zipWith(visited(), (response, visited) -> {
                    response.setVisited(visited);
                    return response;
                })
                .zipWith(recommended(), (response, recommendations) -> {
                    response.setRecommended(recommendations);
                    return response;
                })
                .observeOn(Schedulers.io())
                .subscribe(response -> {
                    response.setProcessingTime((System.nanoTime() - time) / 1000000);
                    deffered.setResult(response);
                });

        return deffered;
    }

    private Observable<List<Recommendation>> recommended() {
        final Observable<Destination> recommended =
                toRxObservableFromReccommend()
                        .onErrorReturn(throwable -> Collections.emptyList())
                        .flatMap(Observable::from)
                        .cache();

        final Observable<Forecast> forecasts = recommended.flatMap(destination ->
                getForcast(destination.getDestination())
                        .onErrorReturn(throwable -> new Forecast(destination.getDestination(), "N/A")));

        final Observable<Calculation> calculations = recommended.flatMap(destination ->
                        price("moon", destination.getDestination())
                        .onErrorReturn(throwable -> new Calculation("Moon", destination.getDestination(), -1)));

        return Observable.zip(recommended, forecasts, calculations, Recommendation::new).toList();
    }

    private Observable<List<Destination>> visited() {
        String url = "http://localhost:8082/remote/destination/visited";
        Observable<List<Destination>> visited = toRxObserable(url);
        return visited;
    }

    private Observable<List<Destination>> toRxObserable(String url) {
        ParameterizedTypeReference<List<Destination>> responseType = new ParameterizedTypeReference<List<Destination>>() {};
        HttpEntity<String> requestEntity = new HttpEntity<>("params", getHttpHeaders());
        org.springframework.util.concurrent.ListenableFuture<ResponseEntity<List<Destination>>> result = asyncRestTemplate().exchange(url, HttpMethod.GET, requestEntity, responseType);
        return toObservable(result).map(responseEntity -> responseEntity.getBody());
    }

    private Observable<List<Destination>> toRxObservableFromReccommend() {
        String url = "http://localhost:8082/remote/destination/recommended";
        return toRxObserable(url);
    }

    private Observable<Forecast> getForcast(String destination) {
        String url = "http://localhost:8083/remote/forecast/%s";
        ParameterizedTypeReference<Forecast> responseType = new ParameterizedTypeReference<Forecast>() {};
        HttpEntity<String> requestEntity = new HttpEntity<>("params", getHttpHeaders());
        org.springframework.util.concurrent.ListenableFuture<ResponseEntity<Forecast>> result = asyncRestTemplate().exchange(String.format(url, destination), HttpMethod.GET, requestEntity, responseType);
        return toObservable(result).map(entity -> entity.getBody());
    }

    private Observable<Calculation> price(String from, String to) {
        String url = "http://localhost:8081/remote/calculation/from/%s/to/%s";
        ParameterizedTypeReference<Calculation> responseType = new ParameterizedTypeReference<Calculation>() {};
        HttpEntity<String> requestEntity = new HttpEntity<>("params", getHttpHeaders());
        org.springframework.util.concurrent.ListenableFuture<ResponseEntity<Calculation>> result = asyncRestTemplate().exchange(String.format(url, from, to), HttpMethod.GET, requestEntity, responseType);
        return toObservable(result).map(entity -> entity.getBody());
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("User", "RxJava");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private AsyncRestTemplate asyncRestTemplate() {
        return new AsyncRestTemplate();
    }

}
