package futures.messages;


import futures.model.AgentResponse;

public class TravelResponse {

    private AgentResponse agentResponse;

    public TravelResponse(AgentResponse agentResponse){
        this.agentResponse = agentResponse;
    }

    public AgentResponse getAgentResponse() {
        return this.agentResponse;
    }
}
