package futures;

public class Util {
    public static void delay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            //Ignore
        }
    }
}
