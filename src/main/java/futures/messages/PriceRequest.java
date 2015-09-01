package futures.messages;


public class PriceRequest {
    private String from;
    private String to;

    public PriceRequest(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }
}
