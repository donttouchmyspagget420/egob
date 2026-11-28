package deez.nuts.egob.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import deez.nuts.egob.R;
import deez.nuts.egob.database.Database;
import deez.nuts.egob.database.entities.Document;
import deez.nuts.egob.database.entities.Profile;
import deez.nuts.egob.database.entities.Service;
import deez.nuts.egob.database.pojos.RequestWithService;
import deez.nuts.egob.util.FileStorage;
import deez.nuts.egob.util.RequestStatus;
import deez.nuts.egob.util.SessionManager;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class AppRepositoryTest {

    private Context context;
    private Database database;
    private AppRepository repository;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        SessionManager.clear(context);
        database = Room.inMemoryDatabaseBuilder(context, Database.class)
                .allowMainThreadQueries()
                .build();
        repository = AppRepository.createForTest(context, database);
        repository.seedBlocking();
    }

    @After
    public void tearDown() {
        SessionManager.clear(context);
        database.close();
    }

    @Test
    public void demoCitizenCanAuthenticate() {
        Result<AuthSession> result = new Result<>();
        repository.login("NAT-8829-4102-X", "egob2026", true, result);

        assertNull(result.error);
        assertNotNull(result.value);
        assertEquals("Elena Vargas", result.value.profile.fullName);
        assertEquals("admin", result.value.user.role);
        assertTrue(SessionManager.currentUserId(context) > 0);

        Result<AuthSession> restored = new Result<>();
        repository.currentSession(restored);
        assertNotNull(restored.value);
        assertEquals("Elena Vargas", restored.value.profile.fullName);
    }

    @Test
    public void invalidCredentialsAreRejected() {
        Result<AuthSession> result = new Result<>();
        repository.login("NAT-8829-4102-X", "incorrecta", true, result);

        assertNull(result.value);
        assertNotNull(result.error);
        assertTrue(result.error.contains("Credenciales"));
    }

    @Test
    public void registrationCreatesLoginAndProfile() {
        Registration registration = new Registration("NIN-5555-1111-22", "Ciudadano Prueba",
                "01/01/1990", "+1 555 000 1111",
                "Distrito 01 - Metropolitano Capital (C.P. 10024)", "claveSegura1");
        Result<AuthSession> registrationResult = new Result<>();
        repository.register(registration, registrationResult);

        assertNull(registrationResult.error);
        assertNotNull(registrationResult.value);
        assertEquals("Ciudadano Prueba", registrationResult.value.profile.fullName);
        assertEquals("citizen", registrationResult.value.user.role);

        repository.logout();
        Result<AuthSession> login = new Result<>();
        repository.login("NIN-5555-1111-22", "claveSegura1", false, login);
        assertNull(login.error);
        assertEquals("Ciudadano Prueba", login.value.profile.fullName);
    }

    @Test
    public void bookmarksToggleAndPersist() {
        AuthSession session = login("NAT-8829-4102-X");
        Result<List<Service>> servicesResult = new Result<>();
        repository.getServices(session.user.id, servicesResult);
        assertNull(servicesResult.error);

        Service passport = null;
        for (Service service : servicesResult.value) {
            if (service.name.toLowerCase().contains("pasaporte")) {
                passport = service;
                break;
            }
        }
        assertNotNull(passport);
        assertTrue(passport.bookmarked);

        Result<Boolean> toggled = new Result<>();
        repository.toggleBookmark(session.user.id, passport.id, toggled);
        assertFalse(toggled.value);

        Result<Boolean> restored = new Result<>();
        repository.toggleBookmark(session.user.id, passport.id, restored);
        assertTrue(restored.value);
    }

    @Test
    public void serviceCatalogueCoversEveryCategoryFilter() {
        AuthSession session = login("NAT-8829-4102-X");
        Result<List<Service>> result = new Result<>();
        repository.getServices(session.user.id, result);

        assertNull(result.error);
        Set<String> categories = new HashSet<>();
        for (Service service : result.value) {
            categories.add(service.category);
        }
        for (String expected : new String[]{"civil", "transport", "housing", "taxes", "health"}) {
            assertTrue("Falta la categoría " + expected, categories.contains(expected));
        }
    }

    @Test
    public void homeQuickActionsResolveRealServices() {
        String[] titles = {
                context.getString(R.string.service_id_title),
                context.getString(R.string.service_taxes_title),
                context.getString(R.string.service_health_title),
                context.getString(R.string.service_appointments_title)
        };
        for (String title : titles) {
            Result<Service> result = new Result<>();
            repository.findServiceByTitle(title, result);
            assertNull("No se encontró: " + title, result.error);
            assertNotNull(result.value);
            assertEquals(title, result.value.name);
        }
    }

    @Test
    public void workerCanAuthenticateAndUseAdminActions() {
        AuthSession worker = login("NAT-5521-7744-31");

        assertEquals("officer", worker.user.role);
        assertTrue(worker.user.isStaff());

        Result<Integer> reassigned = new Result<>();
        repository.reassignQueue(worker.user.id, "Agente de turno", reassigned);
        assertNull(reassigned.error);
        assertNotNull(reassigned.value);
    }

    @Test
    public void creatingRequestPersistsAndNotifiesCitizen() {
        AuthSession session = login("NAT-8829-4102-X");
        Result<List<Service>> servicesResult = new Result<>();
        repository.getServices(session.user.id, servicesResult);
        Service service = servicesResult.value.get(0);

        Result<Integer> before = new Result<>();
        repository.countUnreadNotifications(session.user.id, before);

        Result<RequestWithService> created = new Result<>();
        repository.createRequest(session.user.id, service.id, "Mensajería segura", 50,
                java.util.Collections.emptyList(), created);

        assertNull(created.error);
        assertNotNull(created.value);
        assertTrue(created.value.request.reference.startsWith("#REQ-"));
        assertEquals(RequestStatus.PENDING, created.value.request.status);

        Result<List<RequestWithService>> requests = new Result<>();
        repository.getRequests(session.user.id, requests);
        assertTrue(requests.value.size() >= 5);
        assertEquals(created.value.request.id, requests.value.get(0).request.id);

        Result<Integer> after = new Result<>();
        repository.countUnreadNotifications(session.user.id, after);
        assertEquals(before.value + 1, (int) after.value);
    }

    @Test
    public void adminStatusChangeNotifiesCitizen() {
        AuthSession admin = login("NAT-8829-4102-X");
        Result<List<RequestWithService>> all = new Result<>();
        repository.getAllRequests(all);

        RequestWithService pending = null;
        for (RequestWithService row : all.value) {
            if (RequestStatus.PENDING.equals(row.request.status)) {
                pending = row;
                break;
            }
        }
        assertNotNull(pending);

        Result<Void> updated = new Result<>();
        repository.updateRequestStatus(admin.user.id, pending.request.id,
                RequestStatus.COMPLETED, "Verificación completada.", updated);
        assertNull(updated.error);

        Result<List<RequestWithService>> refreshed = new Result<>();
        repository.getAllRequests(refreshed);
        RequestWithService resolved = null;
        for (RequestWithService row : refreshed.value) {
            if (row.request.id == pending.request.id) {
                resolved = row;
                break;
            }
        }
        assertNotNull(resolved);
        assertEquals(RequestStatus.COMPLETED, resolved.request.status);

        repository.logout();
        AuthSession citizen = login(pending.request.userId == 2
                ? "NAT-1204-7731-08" : "NAT-6630-1182-40");
        Result<Integer> unread = new Result<>();
        repository.countUnreadNotifications(citizen.user.id, unread);
        assertTrue(unread.value > 0);
    }

    @Test
    public void assistantConversationIsPersisted() {
        AuthSession session = login("NAT-8829-4102-X");
        Result<List<deez.nuts.egob.database.entities.Message>> before = new Result<>();
        repository.getMessages(session.user.id, before);

        Result<ChatExchange> exchange = new Result<>();
        repository.sendAssistantMessage(session.user.id,
                "¿Qué necesito para renovar mi pasaporte?", exchange);

        assertNull(exchange.error);
        assertNotNull(exchange.value.assistantMessage);
        assertEquals(0, exchange.value.serviceId);
        assertEquals(AssistantEngine.SINGLE_REPLY, exchange.value.assistantMessage.message);

        Result<List<deez.nuts.egob.database.entities.Message>> after = new Result<>();
        repository.getMessages(session.user.id, after);
        assertEquals(before.value.size() + 2, after.value.size());
    }

    @Test
    public void documentVaultAddsAndRemovesFiles() {
        AuthSession session = login("NAT-8829-4102-X");
        Result<List<Document>> before = new Result<>();
        repository.getDocuments(session.user.id, before);
        assertEquals(3, before.value.size());

        FileStorage.StoredFile stored = new FileStorage.StoredFile(
                "Contrato.pdf", "/tmp/egob_contrato.pdf", "application/pdf", 2048);
        Result<Document> added = new Result<>();
        repository.addDocument(session.user.id, stored, "Contrato de alquiler", added);
        assertNull(added.error);
        assertNotNull(added.value);

        Result<List<Document>> after = new Result<>();
        repository.getDocuments(session.user.id, after);
        assertEquals(4, after.value.size());

        Result<Void> deleted = new Result<>();
        repository.deleteDocument(session.user.id, added.value, deleted);
        assertNull(deleted.error);

        Result<List<Document>> finalList = new Result<>();
        repository.getDocuments(session.user.id, finalList);
        assertEquals(3, finalList.value.size());
    }

    @Test
    public void notificationPreferencesPersist() {
        AuthSession session = login("NAT-8829-4102-X");
        Result<Void> updated = new Result<>();
        repository.updateNotificationPreferences(session.user.id, false, false, true, updated);
        assertNull(updated.error);

        Result<Profile> profile = new Result<>();
        repository.getProfile(session.user.id, profile);
        assertFalse(profile.value.emergencySms);
        assertFalse(profile.value.taxPush);
        assertTrue(profile.value.healthAlerts);
    }

    @Test
    public void markAllNotificationsReadClearsBadge() {
        AuthSession session = login("NAT-8829-4102-X");
        Result<Integer> before = new Result<>();
        repository.countUnreadNotifications(session.user.id, before);
        assertTrue(before.value > 0);

        Result<Void> read = new Result<>();
        repository.markAllNotificationsRead(session.user.id, read);
        assertNull(read.error);

        Result<Integer> after = new Result<>();
        repository.countUnreadNotifications(session.user.id, after);
        assertEquals(0, (int) after.value);
    }

    @Test
    public void broadcastReachesEveryCitizen() {
        AuthSession admin = login("NAT-8829-4102-X");
        Result<AdminStats> stats = new Result<>();
        repository.getAdminStats(stats);

        Result<Integer> broadcast = new Result<>();
        repository.broadcast(admin.user.id, "Aviso cívico", "Mensaje de prueba", broadcast);
        assertNull(broadcast.error);
        assertEquals(stats.value.citizens, (int) broadcast.value);

        repository.logout();
        AuthSession citizen = login("NAT-1204-7731-08");
        Result<Integer> unread = new Result<>();
        repository.countUnreadNotifications(citizen.user.id, unread);
        assertTrue(unread.value > 0);
    }

    @Test
    public void queueReassignmentUpdatesOpenRequests() {
        AuthSession admin = login("NAT-8829-4102-X");
        Result<Integer> reassigned = new Result<>();
        repository.reassignQueue(admin.user.id, "Agente de turno", reassigned);
        assertNull(reassigned.error);
        assertTrue(reassigned.value >= 0);

        Result<List<RequestWithService>> all = new Result<>();
        repository.getAllRequests(all);
        for (RequestWithService row : all.value) {
            if (RequestStatus.isOpen(row.request.status)) {
                assertEquals("Agente de turno", row.request.assignedOfficer);
            }
        }
    }

    @Test
    public void citizenCannotUseAdminActions() {
        AuthSession citizen = login("NAT-1204-7731-08");

        Result<Integer> broadcast = new Result<>();
        repository.broadcast(citizen.user.id, "Aviso", "Mensaje", broadcast);
        assertNull(broadcast.value);
        assertNotNull(broadcast.error);
        assertTrue(broadcast.error.contains("permisos"));

        Result<Integer> reassign = new Result<>();
        repository.reassignQueue(citizen.user.id, "Agente de turno", reassign);
        assertNull(reassign.value);
        assertNotNull(reassign.error);

        Result<List<RequestWithService>> all = new Result<>();
        repository.getAllRequests(all);
        RequestWithService pending = null;
        for (RequestWithService row : all.value) {
            if (RequestStatus.PENDING.equals(row.request.status)) {
                pending = row;
                break;
            }
        }
        if (pending != null) {
            Result<Void> update = new Result<>();
            repository.updateRequestStatus(citizen.user.id, pending.request.id,
                    RequestStatus.COMPLETED, "Intento no autorizado.", update);
            assertNotNull(update.error);
            assertTrue(update.error.contains("permisos"));
        }
    }

    private AuthSession login(String nin) {
        Result<AuthSession> result = new Result<>();
        repository.login(nin, "egob2026", true, result);
        assertNull(result.error);
        assertNotNull(result.value);
        return result.value;
    }

    private static final class Result<T> implements RepositoryCallback<T> {
        T value;
        String error;

        @Override
        public void onSuccess(T value) {
            this.value = value;
        }

        @Override
        public void onError(String message) {
            this.error = message;
        }
    }
}
