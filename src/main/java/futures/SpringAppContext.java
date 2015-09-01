package futures;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.routing.RouterConfig;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;
import scala.concurrent.ExecutionContext;


import static futures.SpringExtension.SpringExtProvider;

@Service
public class SpringAppContext implements ApplicationContextAware {
    private static volatile ApplicationContext context;

    public static ActorSystem actorSystem() {
        return SpringAppContext.getBean(ActorSystem.class);
    }

    public static ExecutionContext dispatcher() {
        return actorSystem().dispatcher();
    }

    public static ConfigurableApplicationContext getContext() {
        return (ConfigurableApplicationContext) context;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static ActorRef getActor(String name) {
        final ActorSystem system = actorSystem();
        return system.actorOf(SpringExtProvider.get(system).props(name), name);
    }

    public static ActorRef getActorWithRouter(String name, RouterConfig rc) {
        final ActorSystem system = actorSystem();
        return system.actorOf(SpringExtProvider.get(system).props(name).withRouter(rc), name);
    }

    public static <T> T getBean(Class<T> requestedType) {
        if(context == null) return null;
        return context.getBean(requestedType);
    }

}
