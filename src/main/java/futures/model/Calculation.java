package futures.model;


public class Calculation {

    private String from;
    private String to;
    private int price;

    public Calculation() {
    }

    public Calculation(final String from, final String to, final int price) {
        this.from = from;
        this.to = to;
        this.price = price;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(final String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(final String to) {
        this.to = to;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(final int price) {
        this.price = price;
    }
}
