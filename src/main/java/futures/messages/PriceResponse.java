package futures.messages;


import futures.model.Calculation;

public class PriceResponse {
    private Calculation price;

     public PriceResponse(Calculation price){
         this.price = price;
     }

     public Calculation getPrice() {
         return this.price;
     }
}
