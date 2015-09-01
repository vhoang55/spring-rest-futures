package futures.external;

import futures.model.Destination;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.transform;


@SpringBootApplication
@RestController
@RequestMapping("remote/destination")
public class DestinationEngineApp {

    private static final Map<String, List<String>> VISITED = new HashMap<>();

    static {
        VISITED.put("Sync", Helper.getCountries(5));
        VISITED.put("Async", Helper.getCountries(5));
        VISITED.put("Guava", Helper.getCountries(5));
        VISITED.put("RxJava", Helper.getCountries(5));
        VISITED.put("Java8", Helper.getCountries(5));
        VISITED.put("Akka", Helper.getCountries(5));
    }

    @RequestMapping("/visited")
    public List<Destination> visited(@RequestHeader(value="User", defaultValue="KO") String user) {
        Helper.sleep();
        if (!VISITED.containsKey(user)) {
            VISITED.put(user, Helper.getCountries(5));
        }
        return transform(VISITED.get(user), Destination::new);
    }

    @RequestMapping("/recommended")
    public List<Destination> recommended(@RequestHeader(value="User", defaultValue="KO") String user,
                                         @RequestParam(value="limit", defaultValue="5")final int limit) {
        Helper.sleep();

        if (!VISITED.containsKey(user)) {
            VISITED.put(user, Helper.getCountries(5));
        }

        return transform(Helper.getCountries(limit, VISITED.get(user)), Destination::new);
    }
}
