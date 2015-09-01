package futures.external;

import futures.model.Forecast;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@EnableAsync
@RequestMapping("remote/forecast")
public class ForecastEngineApp {

    @RequestMapping("/{destination}")
    public Forecast forecast(@PathVariable("destination") final String destination) {
        // Simulate long-running operation.
        Helper.sleep(350);
        return new Forecast(destination, Helper.getForecast());
    }
}
