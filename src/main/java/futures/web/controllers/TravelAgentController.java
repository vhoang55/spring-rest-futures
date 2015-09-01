package futures.web.controllers;

import futures.model.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.util.Arrays.asList;

@RestController
public class TravelAgentController {

    @RequestMapping("/api/travelSuggestion")
    public AgentResponse suggestSynch() {

        final long time = System.nanoTime();

        final AgentResponse response = new AgentResponse();
        final Queue<String> errors = new ConcurrentLinkedQueue<>();

        response.setVisited(getVisited());

        List<Destination> recommended = getRecommendedDestination();

        final Map<String, Forecast> forecasts = getDestinationsForecast(errors, recommended);
        final Map<String, Calculation> calculations = calculateDestinationsCost(errors, recommended);

        // Recommendations.
        final List<Recommendation> recommendations = recommendDestinations(recommended, forecasts, calculations);

        response.setRecommended(recommendations);
        response.setProcessingTime((System.nanoTime() - time) / 1000000);

        return response;

    }



    private List<Recommendation> recommendDestinations(List<Destination> recommended, Map<String, Forecast> forecasts, Map<String, Calculation> calculations) {
        final List<Recommendation> recommendations = new ArrayList<>(recommended.size());
        for (final Destination dest : recommended) {
            final Forecast fore = forecasts.get(dest.getDestination());
            final Calculation calc = calculations.get(dest.getDestination());
            recommendations.add(new Recommendation(dest.getDestination(), fore != null ? fore.getForecast() : "N/A", calc != null ? calc.getPrice() : -1));
        }
        return recommendations;
    }

    private Map<String, Calculation> calculateDestinationsCost(Queue<String> errors, List<Destination> recommended) {
        final Map<String, Calculation> calculations = new HashMap<>();
        recommended.stream().forEach(destination -> {
            try {
                calculations.put(destination.getDestination(), requestPricing("moon", destination.getDestination()));
            } catch (final Throwable throwable) {
                errors.offer("Calculation: " + throwable.getMessage());
            }
        });
        return calculations;
    }

    private Map<String, Forecast> getDestinationsForecast(Queue<String> errors, List<Destination> recommended) {
        final Map<String, Forecast> forecasts = new HashMap<>();
        for (final Destination dest : recommended) {
            try {
                forecasts.put(dest.getDestination(), getForecastDestination(dest.getDestination()));
            } catch (final Throwable throwable) {
                errors.offer("Forecast: " + throwable.getMessage());
            }
        }
        return forecasts;
    }

    private List<Destination> getVisited() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(asList(new HeaderRequestInterceptor("User", "Sync")));
        String url = "http://localhost:8082/remote/destination/visited";
        return asList(restTemplate.getForObject(url, Destination[].class));
    }

    private List<Destination> getRecommendedDestination() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(asList(new HeaderRequestInterceptor("User", "Sync")));
        String url = "http://localhost:8082/remote/destination/recommended";
        return asList(restTemplate.getForObject(url, Destination[].class));
    }

    private Forecast getForecastDestination(String destination) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(asList(new HeaderRequestInterceptor("User", "Sync")));
        String url = "http://localhost:8083/remote/forecast/%s";
        return restTemplate.getForObject(String.format(url, destination), Forecast.class);
    }

    private Calculation requestPricing(String from, String to) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(asList(new HeaderRequestInterceptor("User", "Sync")));
        String url = "http://localhost:8081/remote/calculation/from/%s/to/%s";
        return restTemplate.getForObject(String.format(url, from, to), Calculation.class);
    }

}
