package deez.nuts.egob.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class AssistantEngineTest {

    private final AssistantEngine engine = new AssistantEngine();

    @Test
    public void everyQuestionGetsTheSameSinglePhrase() {
        String[] questions = {
                "Hola",
                "¿Qué necesito para renovar mi pasaporte?",
                "¿Cómo va mi solicitud?",
                "¿Dónde actualizo mi domicilio?",
                "¿Qué tiempo hará mañana?"
        };
        for (String question : questions) {
            AssistantEngine.Reply reply = engine.reply(question);
            assertEquals(AssistantEngine.SINGLE_REPLY, reply.text);
            assertEquals(0, reply.serviceId);
        }
    }

    @Test
    public void replyIsASinglePhrase() {
        AssistantEngine.Reply reply = engine.reply("Cualquier consulta");
        assertFalse(reply.text.contains("\n"));
        assertFalse(reply.text.contains(". "));
    }
}
