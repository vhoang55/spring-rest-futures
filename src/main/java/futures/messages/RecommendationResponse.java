package futures.messages;


import futures.model.Destination;

import java.io.Serializable;
import java.util.List;

public class RecommendationResponse implements Serializable {
    private final List<Destination> recommendDestination;

    public RecommendationResponse(List<Destination> recommendDestination){
        this.recommendDestination = recommendDestination;
    }

    public List<Destination> getRecommendDestination() {
        return this.recommendDestination;
    }
}