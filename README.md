

SpringBoot Rest comibines with JVM Futures from the following libraries
*Guava
*Rxjava
*Akka Futures(and actor)
*Java8 Completable Futures

Compare the Synchronous version vs different Asynchronous, the response time is significantly reduced from ~5 seconds to 1 - 1.5 seconds

To run the project, clone it, and import it into your favorite IDE.

The main entry point of the application is

SpringBootFuturesSimulation,

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

}


this will launch up to 4 instances of embedded tomcats (mainly we want to simulate making external web service calls)
make sure ports 8080 - 8084 are clear...

Once the application startup, navigate to http://localhost:8080/index.html to see the application

once you click on the Load Data Button on each tab, an angularjs controller will fetch the data from the SpringRest controller
and render the response with the processing time for each call.


here is a simple response from server:

<pre>

{
  "visited": [
    {"destination": "Denmark"},
    {"destination": "Haiti"},
    {"destination": "Lebanon"},
    {"destination": "Vietnam"},
    {"destination": "Dominica"}
  ],
  "recommended": [
    {
      "destination": "Rwanda",
      "forecast": "Storm",
      "price": 330
    },
    {
      "destination": "Kenya",
      "forecast": "Snow",
      "price": 2149
    },
    {
      "destination": "St. Vincent & The Grenadines",
      "forecast": "Snow",
      "price": 4537
    },
    {
      "destination": "Palau",
      "forecast": "Sleet",
      "price": 6669
    },
    {
      "destination": "Bangladesh",
      "forecast": "Scattered Showers",
      "price": 7766
    }
  ],
  "processingTime": 1464
}
</pre>

I will write some blog entries about SpringBoot, and and composing Futures with Guava, Java8, Rxjava, Akka shortly..

the blog could be found at: https://randomthought2015.wordpress.com/




