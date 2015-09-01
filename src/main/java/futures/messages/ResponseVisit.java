package futures.messages;

import futures.model.Destination;

import java.io.Serializable;
import java.util.List;

public class ResponseVisit implements Serializable {
    private final List<Destination> visitedDestination;

    public ResponseVisit(List<Destination> visitedDestination){
        this.visitedDestination = visitedDestination;
    }

    public List<Destination> getVisitedDestination() {
        return this.visitedDestination;
    }
}