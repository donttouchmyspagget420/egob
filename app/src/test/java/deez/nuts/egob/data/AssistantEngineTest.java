package deez.nuts.egob.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import deez.nuts.egob.database.entities.Request;
import deez.nuts.egob.database.entities.Service;
import deez.nuts.egob.database.pojos.RequestWithService;
import deez.nuts.egob.util.RequestStatus;

public class AssistantEngineTest {

    private final AssistantEngine engine = new AssistantEngine();

    @Test
    public void greetingIsDeterministic() {
        AssistantEngine.Reply reply = engine.reply("Hola, buenas tardes",
                Collections.emptyList(), Collections.emptyList());
        assertTrue(reply.text.contains("eGobBot"));
        assertEquals(0, reply.serviceId);
    }

    @Test
    public void findsServiceByNaturalLanguage() {
        Service passport = service(7, "Renovación de pasaporte", "Registro Civil",
                "civil", "Pasaporte vigente y fotografía.");
        List<Service> services = Collections.singletonList(passport);

        AssistantEngine.Reply reply = engine.reply(
                "¿Qué necesito para renovar mi pasaporte?", services, Collections.emptyList());

        assertEquals(7, reply.serviceId);
        assertTrue(reply.text.contains("Renovación de pasaporte"));
        assertTrue(reply.text.contains("Pasaporte vigente"));
    }

    @Test
    public void summarisesCitizenRequests() {
        RequestWithService row = new RequestWithService();
        row.request = new Request();
        row.request.reference = "#REQ-90001";
        row.request.status = RequestStatus.ACTION;
        row.serviceName = "Licencia de conducir";
        List<RequestWithService> requests = new ArrayList<>();
        requests.add(row);

        AssistantEngine.Reply reply = engine.reply("¿Cómo va mi solicitud?",
                Collections.emptyList(), requests);

        assertTrue(reply.text.contains("#REQ-90001"));
        assertTrue(reply.text.contains("Licencia de conducir"));
        assertTrue(reply.text.contains("Acción requerida"));
    }

    @Test
    public void unknownQuestionExplainsAvailableTopics() {
        AssistantEngine.Reply reply = engine.reply("¿Qué tiempo hará mañana?",
                Collections.emptyList(), Collections.emptyList());
        assertTrue(reply.text.contains("No encontré"));
    }

    @Test
    public void normalizesAccentsAndPunctuation() {
        assertEquals("donde esta mi pasaporte",
                AssistantEngine.normalize("¿Dónde está mi pasaporte?"));
    }

    private static Service service(int id, String name, String authority, String category,
                                   String requirements) {
        Service service = new Service();
        service.id = id;
        service.name = name;
        service.authority = authority;
        service.category = category;
        service.description = "Trámite de identidad.";
        service.requiredDocuments = requirements;
        service.fee = 45;
        service.time = "2 días";
        return service;
    }
}
