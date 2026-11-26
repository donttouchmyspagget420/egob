package deez.nuts.egob.data;

import deez.nuts.egob.database.entities.Message;

public class ChatExchange {
    public final Message userMessage;
    public final Message assistantMessage;
    public final int serviceId;

    public ChatExchange(Message userMessage, Message assistantMessage) {
        this(userMessage, assistantMessage, 0);
    }

    public ChatExchange(Message userMessage, Message assistantMessage, int serviceId) {
        this.userMessage = userMessage;
        this.assistantMessage = assistantMessage;
        this.serviceId = serviceId;
    }
}
