package futures.external;


import futures.model.Calculation;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Random;


@SpringBootApplication
@RestController
@RequestMapping("remote/calculation")
public class CalculationEngineApp {

    @RequestMapping(value="/from/{from}/to/{to}", method= RequestMethod.GET)
    public DeferredResult<Calculation> calculation(@PathVariable("from")final String from,
                                   @PathVariable("to") final String to) {
        Helper.sleep(350);
        Calculation calculation = new Calculation(from, to, new Random().nextInt(10000));
        DeferredResult<Calculation> deferred = new DeferredResult<>(90000);
        deferred.setResult(calculation);
        return  deferred;
    }


}
