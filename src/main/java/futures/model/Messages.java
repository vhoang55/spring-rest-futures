package futures.model;

import java.util.List;


public class Messages {

    public List<Message> messages;

    public Messages(List<Message> messages){
        this.messages = messages;
    }

    public List<Message> getMessages() {
        return this.messages;
    }
}
