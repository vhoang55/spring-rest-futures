package futures;


import akka.actor.ActorSystem;
import futures.external.CalculationEngineApp;
import futures.external.DestinationEngineApp;
import futures.external.ForecastEngineApp;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class SpringBootFuturesSimulation {

    public static void main(String[] args)  {

        Runtime.getRuntime().addShutdownHook(new Thread(SpringBootFuturesSimulation::shutdown));

        new SpringApplicationBuilder(SpringWebFuturesApplication.class)
                .showBanner(false)
                .properties("server.port=${main.port}")
                .run();


        new SpringApplicationBuilder(CalculationEngineApp.class)
                .showBanner(false)
                .properties("server.port=${calculationEngine.port}")
                .run();

        new SpringApplicationBuilder(DestinationEngineApp.class)
                .showBanner(false)
                .properties("server.port=${destinationEngine.port}")
                .run();


        new SpringApplicationBuilder(ForecastEngineApp.class)
                .showBanner(false)
                .properties("server.port=${forecastEngine.port}")
                .run();

    }

    private static void shutdown() {
        final ActorSystem actorSystem = SpringAppContext.actorSystem();
        final ConfigurableApplicationContext context = SpringAppContext.getContext();

        if (actorSystem != null) actorSystem.shutdown();
        if (context != null) context.close();
    }

}
