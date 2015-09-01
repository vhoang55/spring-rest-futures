package futures;

import akka.actor.ActorSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@SpringBootApplication
public class SpringWebFuturesApplication extends WebMvcConfigurerAdapter {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public ActorSystem actorSystem() {
        ActorSystem system = ActorSystem.create(AkkaActors.SYSTEM_NAME);
        SpringExtension.SpringExtProvider.get(system).initialize(applicationContext);
        return system;
    }
}
