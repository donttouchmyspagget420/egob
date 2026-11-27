package deez.nuts.egob.data;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import deez.nuts.egob.R;
import deez.nuts.egob.database.Database;
import deez.nuts.egob.database.entities.Attachment;
import deez.nuts.egob.database.entities.AuditLog;
import deez.nuts.egob.database.entities.Department;
import deez.nuts.egob.database.entities.Document;
import deez.nuts.egob.database.entities.DocumentUser;
import deez.nuts.egob.database.entities.Message;
import deez.nuts.egob.database.entities.MessageUser;
import deez.nuts.egob.database.entities.Notification;
import deez.nuts.egob.database.entities.NotificationUser;
import deez.nuts.egob.database.entities.Profile;
import deez.nuts.egob.database.entities.Request;
import deez.nuts.egob.database.entities.Role;
import deez.nuts.egob.database.entities.Service;
import deez.nuts.egob.database.entities.ServiceUser;
import deez.nuts.egob.database.entities.User;
import deez.nuts.egob.database.entities.Worker;
import deez.nuts.egob.database.pojos.RequestWithService;
import deez.nuts.egob.security.PasswordHasher;
import deez.nuts.egob.util.FileStorage;
import deez.nuts.egob.util.Formatters;
import deez.nuts.egob.util.PdfExporter;
import deez.nuts.egob.util.RequestStatus;
import deez.nuts.egob.util.SessionManager;

/**
 * Single source of truth for the application. All Room access runs on a private IO executor and
 * callbacks are delivered on the main thread, except in synchronous test mode.
 */
public final class AppRepository {

    private static final String TAG = "AppRepository";
    private static final Pattern POSTAL_CODE = Pattern.compile("C\\.P\\.\\s*(\\d+)");
    private static final AtomicInteger REFERENCE_COUNTER = new AtomicInteger(17);

    private static volatile AppRepository instance;

    private final Context appContext;
    private final Database database;
    private final ExecutorService ioExecutor;
    private final Handler mainHandler;
    private final boolean synchronous;

    private interface Task<T> {
        T run() throws Exception;
    }

    private AppRepository(Context context, Database database, boolean synchronous) {
        this.appContext = context.getApplicationContext();
        this.database = database;
        this.synchronous = synchronous;
        this.ioExecutor = synchronous ? null : Executors.newSingleThreadExecutor();
        this.mainHandler = synchronous ? null : new Handler(Looper.getMainLooper());
        if (!synchronous) {
            ioExecutor.execute(() -> {
                try {
                    seedIfNeeded();
                } catch (Exception e) {
                    Log.e(TAG, "No se pudo preparar la base de datos", e);
                }
            });
        }
    }

    public static AppRepository get(Context context) {
        if (instance == null) {
            synchronized (AppRepository.class) {
                if (instance == null) {
                    instance = new AppRepository(context, Database.getInstance(context), false);
                }
            }
        }
        return instance;
    }

    /** Creates a deterministic repository for tests. Call {@link #seedBlocking()} when needed. */
    public static AppRepository createForTest(Context context, Database database) {
        return new AppRepository(context, database, true);
    }

    public void seedBlocking() {
        if (synchronous || ioExecutor == null) {
            seedIfNeeded();
            return;
        }
        try {
            ioExecutor.submit(this::seedIfNeeded).get();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo preparar la base de datos", e);
        }
    }

    private <T> void execute(RepositoryCallback<T> callback, Task<T> task) {
        Runnable work = () -> {
            try {
                T value = task.run();
                deliver(() -> callback.onSuccess(value));
            } catch (Throwable error) {
                String message = error.getMessage();
                if (message == null || message.trim().isEmpty()) {
                    message = "Ocurrió un error inesperado. Inténtalo de nuevo.";
                }
                String finalMessage = message;
                deliver(() -> callback.onError(finalMessage));
            }
        };
        if (synchronous) {
            work.run();
        } else {
            ioExecutor.execute(work);
        }
    }

    private void deliver(Runnable runnable) {
        if (synchronous) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }

    // ---------------------------------------------------------------- authentication

    public void login(String identifier, String password, boolean rememberDevice,
                      RepositoryCallback<AuthSession> callback) {
        execute(callback, () -> {
            String value = identifier == null ? "" : identifier.trim();
            if (value.isEmpty() || password == null || password.isEmpty()) {
                throw new IllegalArgumentException("Ingresa tu NIN y tu código de acceso.");
            }
            User user = database.userDao().findByIdentifier(value);
            if (user == null || !PasswordHasher.verify(password, user.password)) {
                throw new IllegalArgumentException("Credenciales no válidas. Verifica tu NIN y tu clave.");
            }
            Profile profile = user.profileId == null ? null : database.profileDao().getById(user.profileId);
            if (profile == null) {
                throw new IllegalStateException("Tu perfil ciudadano no está disponible.");
            }
            SessionManager.start(appContext, user.id, rememberDevice);
            database.auditLogDao().insert(audit(user.id, "login", "user", user.id,
                    "Inicio de sesión verificado con contraseña."));
            return new AuthSession(user, profile);
        });
    }

    public void loginWithDevice(RepositoryCallback<AuthSession> callback) {
        execute(callback, () -> {
            int userId = SessionManager.currentUserId(appContext);
            if (userId <= 0) {
                throw new IllegalStateException("No hay una llave de acceso vinculada a este dispositivo. "
                        + "Inicia sesión con tu identidad nacional.");
            }
            User user = database.userDao().getById(userId);
            Profile profile = user == null || user.profileId == null
                    ? null : database.profileDao().getById(user.profileId);
            if (user == null || profile == null) {
                throw new IllegalStateException("La llave de acceso ya no es válida. Inicia sesión de nuevo.");
            }
            SessionManager.start(appContext, user.id, true);
            return new AuthSession(user, profile);
        });
    }

    public void currentSession(RepositoryCallback<AuthSession> callback) {
        execute(callback, () -> {
            int userId = SessionManager.currentUserId(appContext);
            if (userId <= 0) {
                throw new IllegalStateException("Sesión no iniciada.");
            }
            User user = database.userDao().getById(userId);
            Profile profile = user == null || user.profileId == null
                    ? null : database.profileDao().getById(user.profileId);
            if (user == null || profile == null) {
                throw new IllegalStateException("Tu perfil ciudadano no está disponible.");
            }
            return new AuthSession(user, profile);
        });
    }

    public void logout() {
        SessionManager.clear(appContext);
    }

    public void register(Registration registration, RepositoryCallback<AuthSession> callback) {
        execute(callback, () -> {
            String nin = registration.nin.trim().toUpperCase(Locale.US);
            String fullName = registration.fullName.trim();
            if (nin.isEmpty() || fullName.isEmpty() || registration.password.length() < 8) {
                throw new IllegalArgumentException("Completa los datos obligatorios y usa una clave de 8 caracteres o más.");
            }
            if (database.userDao().getByNin(nin) != null) {
                throw new IllegalArgumentException("Ya existe una identidad registrada con ese NIN.");
            }
            String email = nin.toLowerCase(Locale.US).replaceAll("[^a-z0-9]", "") + "@egob.gob";
            if (database.userDao().getByEmail(email) != null) {
                throw new IllegalArgumentException("Ya existe una identidad registrada con ese NIN.");
            }

            Profile profile = new Profile();
            profile.dni = nin;
            profile.phone = registration.phone.trim();
            profile.adress = "Domicilio pendiente de verificación";
            profile.code = postalCode(registration.district);
            profile.fullName = fullName;
            profile.dateOfBirth = registration.dateOfBirth;
            profile.city = "Capital Cívica";
            profile.district = registration.district;
            profile.maritalStatus = "Soltero/a";
            profile.pollingPlace = "Pendiente de asignación";
            profile.tier = "Nivel 3";
            profile.memberSince = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
            profile.verified = true;
            profile.biometricEnabled = false;
            profile.twoFactorEnabled = true;
            profile.emergencySms = true;
            profile.taxPush = true;
            profile.healthAlerts = false;
            profile.language = "Español (oficial)";
            profile.avatarInitials = Formatters.initials(fullName);
            int profileId = (int) database.profileDao().insert(profile);

            User user = new User();
            user.name = fullName;
            user.email = email;
            user.password = PasswordHasher.hash(registration.password);
            user.nin = nin;
            user.role = "citizen";
            user.profileId = profileId;
            user.createdAt = System.currentTimeMillis();
            int userId = (int) database.userDao().insert(user);

            Notification welcome = new Notification();
            welcome.category = "legal";
            welcome.title = "Identidad digital eGob creada";
            welcome.body = "Tu registro ciudadano quedó activo. Ya puedes solicitar servicios y usar tu billetera digital.";
            welcome.timeLabel = "Ahora";
            welcome.actionLabel = "Explorar servicios";
            welcome.read = false;
            welcome.dismissed = false;
            welcome.createdAt = System.currentTimeMillis();
            welcome.relatedType = "none";
            welcome.relatedId = 0;
            int notificationId = (int) database.notificationDao().insert(welcome);
            NotificationUser notificationUser = new NotificationUser();
            notificationUser.userId = userId;
            notificationUser.notificationId = notificationId;
            database.notificationUserDao().insert(notificationUser);

            Message message = new Message();
            message.role = "assistant";
            message.message = "Hola, " + firstName(fullName) + ". Soy eGobBot.";
            message.createdAt = System.currentTimeMillis();
            int messageId = (int) database.messageDao().insert(message);
            MessageUser messageUser = new MessageUser();
            messageUser.userId = userId;
            messageUser.messageId = messageId;
            database.messageUserDao().insert(messageUser);

            database.auditLogDao().insert(audit(userId, "register", "user", userId,
                    "Registro ciudadano completado con NIN " + nin + "."));

            SessionManager.start(appContext, userId, true);
            return new AuthSession(user, profile);
        });
    }

    // ---------------------------------------------------------------- services

    public void getServices(int userId, RepositoryCallback<List<Service>> callback) {
        execute(callback, () -> {
            List<Service> services = database.serviceDao().getActive();
            Set<Integer> bookmarks = new HashSet<>(database.serviceUserDao().getServiceIds(userId));
            for (Service service : services) {
                service.bookmarked = bookmarks.contains(service.id);
            }
            return services;
        });
    }

    public void getService(int serviceId, RepositoryCallback<Service> callback) {
        execute(callback, () -> {
            Service service = database.serviceDao().getById(serviceId);
            if (service == null) {
                throw new IllegalArgumentException("El servicio ya no está disponible.");
            }
            return service;
        });
    }

    public void findServiceByTitle(String title, RepositoryCallback<Service> callback) {
        execute(callback, () -> {
            if (title == null || title.trim().isEmpty()) {
                throw new IllegalArgumentException("Servicio no encontrado.");
            }
            for (Service service : database.serviceDao().getAll()) {
                if (service.name != null && service.name.equalsIgnoreCase(title.trim())) {
                    return service;
                }
            }
            for (Service service : database.serviceDao().getAll()) {
                if (service.name != null && service.name.toLowerCase(Locale.getDefault())
                        .contains(title.trim().toLowerCase(Locale.getDefault()))) {
                    return service;
                }
            }
            throw new IllegalArgumentException("Servicio no encontrado.");
        });
    }

    public void toggleBookmark(int userId, int serviceId, RepositoryCallback<Boolean> callback) {
        execute(callback, () -> {
            boolean bookmarked;
            if (database.serviceUserDao().countBookmark(userId, serviceId) > 0) {
                database.serviceUserDao().deleteBookmark(userId, serviceId);
                bookmarked = false;
            } else {
                ServiceUser link = new ServiceUser();
                link.userId = userId;
                link.serviceId = serviceId;
                database.serviceUserDao().insert(link);
                bookmarked = true;
            }
            database.auditLogDao().insert(audit(userId, bookmarked ? "bookmark_add" : "bookmark_remove",
                    "service", serviceId, "Favorito actualizado."));
            return bookmarked;
        });
    }

    // ---------------------------------------------------------------- requests

    public void getRequests(int userId, RepositoryCallback<List<RequestWithService>> callback) {
        execute(callback, () -> database.requestDao().getByUserWithService(userId));
    }

    public void getAllRequests(RepositoryCallback<List<RequestWithService>> callback) {
        execute(callback, () -> database.requestDao().getAllWithService());
    }

    public void createRequest(int userId, int serviceId, String deliveryMethod, double fee,
                              List<FileStorage.StoredFile> files,
                              RepositoryCallback<RequestWithService> callback) {
        execute(callback, () -> {
            Service service = database.serviceDao().getById(serviceId);
            if (service == null) {
                throw new IllegalArgumentException("El servicio ya no está disponible.");
            }
            User user = database.userDao().getById(userId);
            if (user == null) {
                throw new IllegalStateException("Sesión no válida. Inicia sesión de nuevo.");
            }
            long now = System.currentTimeMillis();
            Request request = new Request();
            request.servicesId = service.id;
            request.userId = userId;
            request.status = RequestStatus.PENDING;
            request.time = service.time;
            request.reference = newReference();
            request.fee = fee;
            request.deliveryMethod = deliveryMethod;
            request.departmentId = service.departmentId;
            request.createdAt = now;
            request.updatedAt = now;
            request.priority = service.badge == null ? "normal" : "alta";
            request.assignedOfficer = "Mesa de operaciones";
            request.notes = "Solicitud creada desde el portal eGob.";
            int requestId = (int) database.requestDao().insert(request);

            if (files != null) {
                for (FileStorage.StoredFile file : files) {
                    Attachment attachment = new Attachment();
                    attachment.requestId = requestId;
                    attachment.userId = userId;
                    attachment.name = file.name;
                    attachment.path = file.path;
                    attachment.mimeType = file.mimeType;
                    attachment.fileSize = file.size;
                    attachment.createdAt = now;
                    database.attachmentDao().insert(attachment);
                }
            }

            addNotification(userId, "updates", "Solicitud recibida",
                    "Tu solicitud " + request.reference + " para " + service.name
                            + " fue registrada y está pendiente de revisión.",
                    "Ver seguimiento", "request", requestId);
            database.auditLogDao().insert(audit(userId, "request_create", "request", requestId,
                    request.reference + " · " + service.name));
            RequestWithService row = database.requestDao().getWithServiceById(requestId);
            if (row == null) {
                throw new IllegalStateException("La solicitud se creó, pero no se pudo cargar su detalle.");
            }
            return row;
        });
    }

    public void getAttachments(int requestId, RepositoryCallback<List<Attachment>> callback) {
        execute(callback, () -> database.attachmentDao().getByRequest(requestId));
    }

    public void addAttachment(int userId, int requestId, FileStorage.StoredFile file,
                              RepositoryCallback<Attachment> callback) {
        execute(callback, () -> {
            Request request = database.requestDao().getById(requestId);
            if (request == null) {
                throw new IllegalArgumentException("La solicitud ya no existe.");
            }
            Attachment attachment = new Attachment();
            attachment.requestId = requestId;
            attachment.userId = userId;
            attachment.name = file.name;
            attachment.path = file.path;
            attachment.mimeType = file.mimeType;
            attachment.fileSize = file.size;
            attachment.createdAt = System.currentTimeMillis();
            attachment.id = (int) database.attachmentDao().insert(attachment);

            if (RequestStatus.ACTION.equals(request.status)) {
                database.requestDao().updateStatus(requestId, RequestStatus.IN_PROGRESS,
                        System.currentTimeMillis(), request.assignedOfficer);
            }
            addNotification(userId, "updates", "Documento recibido",
                    "El documento " + file.name + " se adjuntó a la solicitud " + request.reference
                            + ". La revisión continúa.",
                    "Ver solicitud", "request", requestId);
            database.auditLogDao().insert(audit(userId, "attachment_add", "request", requestId,
                    file.name + " (" + FileStorage.humanSize(file.size) + ")"));
            return attachment;
        });
    }

    /** Copies a content URI into private storage on the IO executor. */
    public void copyFile(Uri uri, RepositoryCallback<FileStorage.StoredFile> callback) {
        execute(callback, () -> FileStorage.copy(appContext, uri));
    }

    public void downloadRequestPdf(int requestId, RepositoryCallback<File> callback) {
        execute(callback, () -> {
            RequestWithService row = database.requestDao().getWithServiceById(requestId);
            if (row == null) {
                throw new IllegalArgumentException("La solicitud ya no existe.");
            }
            List<Attachment> attachments = database.attachmentDao().getByRequest(requestId);
            return PdfExporter.exportRequest(appContext, row, attachments);
        });
    }

    public void requestCorrection(int userId, String note, RepositoryCallback<Request> callback) {
        execute(callback, () -> {
            Service service = database.serviceDao().getById(3);
            if (service == null) {
                throw new IllegalStateException("El servicio de corrección no está disponible.");
            }
            long now = System.currentTimeMillis();
            Request request = new Request();
            request.servicesId = service.id;
            request.userId = userId;
            request.status = RequestStatus.PENDING;
            request.time = "1 día hábil";
            request.reference = newReference();
            request.fee = 0;
            request.deliveryMethod = "Revisión documental";
            request.departmentId = service.departmentId;
            request.createdAt = now;
            request.updatedAt = now;
            request.priority = "normal";
            request.assignedOfficer = "Registro Civil";
            request.notes = note == null || note.trim().isEmpty()
                    ? "Solicitud de corrección de datos del registro." : note.trim();
            int requestId = (int) database.requestDao().insert(request);
            addNotification(userId, "legal", "Corrección de registro iniciada",
                    "Recibimos tu solicitud " + request.reference + ". Un agente del Registro Civil revisará los cambios.",
                    "Ver solicitud", "request", requestId);
            database.auditLogDao().insert(audit(userId, "correction_request", "request", requestId,
                    request.notes));
            return request;
        });
    }

    // ---------------------------------------------------------------- documents

    public void getDocuments(int userId, RepositoryCallback<List<Document>> callback) {
        execute(callback, () -> database.documentUserDao().getDocumentsForUser(userId));
    }

    public void addDocument(int userId, FileStorage.StoredFile file, String title,
                            RepositoryCallback<Document> callback) {
        execute(callback, () -> {
            Document document = new Document();
            document.path = file.path;
            document.title = title == null || title.trim().isEmpty() ? file.name : title.trim();
            document.type = "uploaded";
            document.holderName = currentProfile(userId).fullName;
            document.documentNumber = "UPL-" + (System.currentTimeMillis() % 1000000);
            document.issuedAt = Formatters.date(System.currentTimeMillis());
            document.expiresAt = "Sin vencimiento";
            document.verified = false;
            document.mimeType = file.mimeType;
            document.fileSize = file.size;
            document.createdAt = System.currentTimeMillis();
            document.id = (int) database.documentDao().insert(document);

            DocumentUser link = new DocumentUser();
            link.userId = userId;
            link.documentId = document.id;
            database.documentUserDao().insert(link);

            addNotification(userId, "legal", "Documento añadido a la bóveda",
                    document.title + " se guardó en tu billetera digital.",
                    "Abrir bóveda", "document", document.id);
            database.auditLogDao().insert(audit(userId, "document_add", "document", document.id,
                    document.title + " (" + FileStorage.humanSize(file.size) + ")"));
            return document;
        });
    }

    public void deleteDocument(int userId, Document document, RepositoryCallback<Void> callback) {
        execute(callback, () -> {
            database.documentUserDao().deleteByDocumentId(document.id);
            database.documentDao().delete(document);
            FileStorage.deleteQuietly(document.path);
            database.auditLogDao().insert(audit(userId, "document_delete", "document", document.id,
                    document.title));
            return null;
        });
    }

    public void downloadProfilePdf(int userId, RepositoryCallback<File> callback) {
        execute(callback, () -> {
            Profile profile = currentProfile(userId);
            return PdfExporter.exportProfile(appContext, profile);
        });
    }

    // ---------------------------------------------------------------- notifications

    public void getNotifications(int userId, RepositoryCallback<List<Notification>> callback) {
        execute(callback, () -> database.notificationUserDao().getActiveForUser(userId));
    }

    public void countUnreadNotifications(int userId, RepositoryCallback<Integer> callback) {
        execute(callback, () -> database.notificationUserDao().countUnread(userId));
    }

    public void markAllNotificationsRead(int userId, RepositoryCallback<Void> callback) {
        execute(callback, () -> {
            database.notificationUserDao().markAllRead(userId);
            database.auditLogDao().insert(audit(userId, "notifications_read", "user", userId,
                    "Todas las notificaciones marcadas como leídas."));
            return null;
        });
    }

    public void markNotificationRead(int notificationId) {
        execute(new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void value) {
            }

            @Override
            public void onError(String message) {
            }
        }, () -> {
            database.notificationUserDao().markRead(notificationId);
            return null;
        });
    }

    public void dismissNotification(int userId, int notificationId, RepositoryCallback<Void> callback) {
        execute(callback, () -> {
            database.notificationUserDao().dismiss(notificationId);
            database.auditLogDao().insert(audit(userId, "notification_dismiss", "notification",
                    notificationId, "Aviso descartado."));
            return null;
        });
    }

    // ---------------------------------------------------------------- assistant

    public void getMessages(int userId, RepositoryCallback<List<Message>> callback) {
        execute(callback, () -> {
            List<Message> messages = database.messageUserDao().getMessagesForUser(userId);
            if (messages.isEmpty()) {
                Profile profile = currentProfile(userId);
                Message welcome = new Message();
                welcome.role = "assistant";
                welcome.message = "Hola, " + firstName(profile.fullName) + ". Soy eGobBot.";
                welcome.createdAt = System.currentTimeMillis();
                welcome.id = (int) database.messageDao().insert(welcome);
                MessageUser link = new MessageUser();
                link.userId = userId;
                link.messageId = welcome.id;
                database.messageUserDao().insert(link);
                messages = new ArrayList<>();
                messages.add(welcome);
            }
            return messages;
        });
    }

    public void sendAssistantMessage(int userId, String text, RepositoryCallback<ChatExchange> callback) {
        execute(callback, () -> {
            String clean = text == null ? "" : text.trim();
            if (clean.isEmpty()) {
                throw new IllegalArgumentException("Escribe una consulta para el asistente.");
            }
            Message userMessage = insertMessage(userId, "user", clean);
            AssistantEngine engine = new AssistantEngine();
            AssistantEngine.Reply reply = engine.reply(clean);
            Message botMessage = insertMessage(userId, "assistant", reply.text);
            return new ChatExchange(userMessage, botMessage, reply.serviceId);
        });
    }

    public void sendAssistantAttachment(int userId, String fileName,
                                        RepositoryCallback<ChatExchange> callback) {
        execute(callback, () -> {
            Message userMessage = insertMessage(userId, "user", "Adjunto: " + fileName);
            Message botMessage = insertMessage(userId, "assistant", AssistantEngine.SINGLE_REPLY);
            return new ChatExchange(userMessage, botMessage);
        });
    }

    // ---------------------------------------------------------------- admin

    public void getAdminStats(RepositoryCallback<AdminStats> callback) {
        execute(callback, () -> {
            List<Request> requests = database.requestDao().getAll();
            long startOfDay = startOfToday();
            AdminStats stats = new AdminStats();
            stats.totalRequests = requests.size();
            stats.citizens = database.userDao().count();
            int completed = 0;
            int rejected = 0;
            for (Request request : requests) {
                if (RequestStatus.isOpen(request.status)) {
                    stats.activeRequests++;
                }
                if (RequestStatus.PENDING.equals(request.status)
                        || RequestStatus.ACTION.equals(request.status)) {
                    stats.pendingVerifications++;
                }
                if (RequestStatus.COMPLETED.equals(request.status)) {
                    completed++;
                    stats.completedTotal++;
                    if (request.updatedAt >= startOfDay) {
                        stats.completedToday++;
                    }
                }
                if (RequestStatus.REJECTED.equals(request.status)) {
                    rejected++;
                    stats.rejectedTotal++;
                }
            }
            int denominator = completed + rejected + stats.activeRequests;
            stats.compliancePercent = denominator == 0
                    ? 100.0 : Math.round((completed * 1000.0 / denominator)) / 10.0;
            return stats;
        });
    }

    public void getAuditLogs(int limit, RepositoryCallback<List<AuditLog>> callback) {
        execute(callback, () -> database.auditLogDao().getRecent(limit));
    }

    public void getAuditLogsForUser(int userId, int limit, RepositoryCallback<List<AuditLog>> callback) {
        execute(callback, () -> database.auditLogDao().getForActor(userId, limit));
    }

    public void updateRequestStatus(int adminUserId, int requestId, String newStatus,
                                    String note, RepositoryCallback<Void> callback) {
        execute(callback, () -> {
            requireAdmin(adminUserId);
            Request request = database.requestDao().getById(requestId);
            if (request == null) {
                throw new IllegalArgumentException("La solicitud ya no existe.");
            }
            if (!RequestStatus.canTransition(request.status, newStatus)) {
                throw new IllegalStateException("La solicitud no admite esa transición de estado.");
            }
            User admin = database.userDao().getById(adminUserId);
            String officer = admin == null ? "Mesa de operaciones" : admin.name;
            int updated = database.requestDao().updateStatus(requestId, newStatus,
                    System.currentTimeMillis(), officer);
            if (updated == 0) {
                throw new IllegalStateException("No se pudo actualizar la solicitud.");
            }
            database.auditLogDao().insert(audit(adminUserId, "request_status", "request", requestId,
                    request.reference + ": " + request.status + " -> " + newStatus
                            + (note == null || note.isEmpty() ? "" : " · " + note)));
            addNotification(request.userId, "updates", "Solicitud actualizada",
                    "La solicitud " + request.reference + " cambió a " + RequestStatus.label(newStatus) + "."
                            + (note == null || note.isEmpty() ? "" : " " + note),
                    "Ver solicitud", "request", requestId);
            return null;
        });
    }

    public void reassignQueue(int adminUserId, String officer,
                              RepositoryCallback<Integer> callback) {
        execute(callback, () -> {
            requireAdmin(adminUserId);
            String target = officer == null || officer.trim().isEmpty()
                    ? "Agente de turno" : officer.trim();
            int updated = database.requestDao().reassignOpenRequests(target,
                    System.currentTimeMillis(), RequestStatus.openStatuses());
            database.auditLogDao().insert(audit(adminUserId, "queue_reassign", "request", 0,
                    updated + " solicitudes abiertas reasignadas a " + target + "."));
            return updated;
        });
    }

    public void broadcast(int adminUserId, String title, String body,
                          RepositoryCallback<Integer> callback) {
        execute(callback, () -> {
            requireAdmin(adminUserId);
            List<User> users = database.userDao().getAll();
            long now = System.currentTimeMillis();
            for (User user : users) {
                Notification notification = new Notification();
                notification.category = "updates";
                notification.title = title;
                notification.body = body;
                notification.timeLabel = "Ahora";
                notification.actionLabel = "Ver servicios";
                notification.read = false;
                notification.dismissed = false;
                notification.createdAt = now;
                notification.relatedType = "none";
                notification.relatedId = 0;
                int notificationId = (int) database.notificationDao().insert(notification);
                NotificationUser link = new NotificationUser();
                link.userId = user.id;
                link.notificationId = notificationId;
                database.notificationUserDao().insert(link);
            }
            database.auditLogDao().insert(audit(adminUserId, "broadcast", "notification", 0,
                    title + " · " + body));
            return users.size();
        });
    }

    public void getAuditCount(int userId, RepositoryCallback<Integer> callback) {
        execute(callback, () -> database.auditLogDao().countForActor(userId));
    }

    // ---------------------------------------------------------------- profile

    public void getProfile(int userId, RepositoryCallback<Profile> callback) {
        execute(callback, () -> currentProfile(userId));
    }

    public void updateProfile(int userId, Profile profile, RepositoryCallback<Void> callback) {
        execute(callback, () -> {
            database.profileDao().update(profile);
            database.auditLogDao().insert(audit(userId, "profile_update", "profile", profile.id,
                    "Datos de contacto y preferencias actualizados."));
            return null;
        });
    }

    public void updateNotificationPreferences(int userId, boolean emergencySms, boolean taxPush,
                                              boolean healthAlerts,
                                              RepositoryCallback<Void> callback) {
        execute(callback, () -> {
            Profile profile = currentProfile(userId);
            profile.emergencySms = emergencySms;
            profile.taxPush = taxPush;
            profile.healthAlerts = healthAlerts;
            database.profileDao().update(profile);
            database.auditLogDao().insert(audit(userId, "preferences_update", "profile", profile.id,
                    "Preferencias de notificación actualizadas."));
            return null;
        });
    }

    // ---------------------------------------------------------------- helpers

    private void requireAdmin(int userId) {
        User user = database.userDao().getById(userId);
        if (user == null || !"admin".equals(user.role)) {
            throw new SecurityException("No tienes permisos de administración.");
        }
    }

    private Profile currentProfile(int userId) {
        User user = database.userDao().getById(userId);
        Profile profile = user == null || user.profileId == null
                ? null : database.profileDao().getById(user.profileId);
        if (profile == null) {
            throw new IllegalStateException("Tu perfil ciudadano no está disponible.");
        }
        return profile;
    }

    private Message insertMessage(int userId, String role, String text) {
        Message message = new Message();
        message.role = role;
        message.message = text;
        message.createdAt = System.currentTimeMillis();
        message.id = (int) database.messageDao().insert(message);
        MessageUser link = new MessageUser();
        link.userId = userId;
        link.messageId = message.id;
        database.messageUserDao().insert(link);
        return message;
    }

    private void addNotification(int userId, String category, String title, String body,
                                 String action, String relatedType, int relatedId) {
        Notification notification = new Notification();
        notification.category = category;
        notification.title = title;
        notification.body = body;
        notification.timeLabel = "Ahora";
        notification.actionLabel = action;
        notification.read = false;
        notification.dismissed = false;
        notification.createdAt = System.currentTimeMillis();
        notification.relatedType = relatedType;
        notification.relatedId = relatedId;
        int notificationId = (int) database.notificationDao().insert(notification);
        NotificationUser link = new NotificationUser();
        link.userId = userId;
        link.notificationId = notificationId;
        database.notificationUserDao().insert(link);
    }

    private AuditLog audit(int actorUserId, String action, String entityType, int entityId,
                           String details) {
        AuditLog log = new AuditLog();
        log.actorUserId = actorUserId;
        log.action = action;
        log.entityType = entityType;
        log.entityId = entityId;
        log.details = details;
        log.createdAt = System.currentTimeMillis();
        return log;
    }

    private static String newReference() {
        int value = 90000 + (int) (System.currentTimeMillis() % 9000)
                + REFERENCE_COUNTER.incrementAndGet();
        return "#REQ-" + value;
    }

    private static String firstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "ciudadano";
        }
        return fullName.trim().split("\\s+")[0];
    }

    private static String postalCode(String district) {
        if (district != null) {
            Matcher matcher = POSTAL_CODE.matcher(district);
            if (matcher.find()) {
                return "CP-" + matcher.group(1);
            }
        }
        return "CP-00000";
    }

    private static long startOfToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    // ---------------------------------------------------------------- seed

    private synchronized void seedIfNeeded() {
        if (database.userDao().count() > 0) {
            return;
        }
        long now = System.currentTimeMillis();

        Profile elena = profile("Elena Vargas", "NAT-8829-4102-X", "+1 (555) 389-4412",
                "Calle Los Álamos 742, Sector 4", "CP-10024", "14 oct 1989",
                "Distrito 01 - Territorio Capital", "Casada", "Precinto Metropolitano #14-B",
                "Nivel 4", "2012", "EV", true);
        Profile marcus = profile("Marcus Brody", "NAT-1204-7731-08", "+1 (555) 220-8811",
                "Avenida Central 120, Sector 2", "CP-10021", "02 mar 1984",
                "Distrito 02 - Tierras Altas del Norte", "Soltero", "Escuela Cívica #08",
                "Nivel 3", "2016", "MB", true);
        Profile aisha = profile("Aisha Patel", "NAT-6630-1182-40", "+1 (555) 771-2094",
                "Calle Mercado 45, Sector 3", "CP-30089", "19 jul 1991",
                "Distrito 03 - Cuenca Costera Este", "Casada", "Centro Comunitario #03",
                "Nivel 4", "2014", "AP", true);
        Profile david = profile("David Kim", "NAT-4471-9082-15", "+1 (555) 330-1147",
                "Camino del Valle 8, Sector 5", "CP-41005", "30 nov 1979",
                "Distrito 04 - Sector Valle Occidental", "Casado", "Salón Vecinal #12",
                "Nivel 3", "2011", "DK", true);
        int elenaProfile = (int) database.profileDao().insert(elena);
        int marcusProfile = (int) database.profileDao().insert(marcus);
        int aishaProfile = (int) database.profileDao().insert(aisha);
        int davidProfile = (int) database.profileDao().insert(david);

        Department civil = department("Registro Civil");
        Department transport = department("Transporte y Vehículos");
        Department revenue = department("Ingresos y Catastro");
        Department health = department("Salud y Bienestar Social");
        Department commerce = department("Comercio y Empresas");
        Department housing = department("Vivienda y Tierras");
        int civilId = (int) database.departmentDao().insert(civil);
        int transportId = (int) database.departmentDao().insert(transport);
        int revenueId = (int) database.departmentDao().insert(revenue);
        int healthId = (int) database.departmentDao().insert(health);
        int commerceId = (int) database.departmentDao().insert(commerce);
        int housingId = (int) database.departmentDao().insert(housing);

        int citizenRole = (int) database.roleDao().insert(role("citizen"));
        int adminRole = (int) database.roleDao().insert(role("admin"));
        database.roleDao().insert(role("officer"));

        User elenaUser = user("Elena Vargas", "elena.vargas@egob.gob", "egob2026",
                "NAT-8829-4102-X", "admin", elenaProfile, now - 400L * 24 * 60 * 60 * 1000);
        User marcusUser = user("Marcus Brody", "marcus.brody@egob.gob", "egob2026",
                "NAT-1204-7731-08", "citizen", marcusProfile, now - 300L * 24 * 60 * 60 * 1000);
        User aishaUser = user("Aisha Patel", "aisha.patel@egob.gob", "egob2026",
                "NAT-6630-1182-40", "citizen", aishaProfile, now - 280L * 24 * 60 * 60 * 1000);
        User davidUser = user("David Kim", "david.kim@egob.gob", "egob2026",
                "NAT-4471-9082-15", "citizen", davidProfile, now - 250L * 24 * 60 * 60 * 1000);
        int elenaId = (int) database.userDao().insert(elenaUser);
        int marcusId = (int) database.userDao().insert(marcusUser);
        int aishaId = (int) database.userDao().insert(aishaUser);
        int davidId = (int) database.userDao().insert(davidUser);

        Worker worker = new Worker();
        worker.userId = elenaId;
        worker.departmentId = civilId;
        worker.roleId = adminRole;
        database.workerDao().insert(worker);

        Service passport = service(appContext.getString(R.string.service_passport_title),
                appContext.getString(R.string.service_passport_desc),
                appContext.getString(R.string.service_passport_authority), "civil", null, 45.0,
                appContext.getString(R.string.service_days_2_5), "10 años",
                "NIN, fotografía biométrica digital, pasaporte vigente y comprobante de residencia.",
                "Mensajería segura o recogida en oficina", civilId, true);
        Service driver = service(appContext.getString(R.string.service_driver_title),
                appContext.getString(R.string.service_driver_desc),
                appContext.getString(R.string.service_driver_authority), "transport", null, 32.5,
                appContext.getString(R.string.service_driver_time), "5 años",
                "Licencia vigente, examen visual y comprobante de domicilio.",
                "Credencial digital inmediata", transportId, true);
        Service civilRecords = service(appContext.getString(R.string.service_civil_title),
                appContext.getString(R.string.service_civil_desc),
                appContext.getString(R.string.service_civil_authority), "civil",
                appContext.getString(R.string.service_civil_badge), 0,
                appContext.getString(R.string.service_civil_time), "Vigencia permanente",
                "NIN y datos del acta a certificar.", "PDF firmado en la billetera", civilId, true);
        Service business = service(appContext.getString(R.string.service_commerce_title),
                appContext.getString(R.string.service_commerce_desc),
                appContext.getString(R.string.service_commerce_authority), "taxes", null, 80,
                appContext.getString(R.string.service_days_3_7), "1 año",
                "Identidad fiscal, domicilio comercial y objeto social.",
                "Resolución digital", commerceId, true);
        Service social = service(appContext.getString(R.string.service_social_title),
                appContext.getString(R.string.service_social_desc),
                appContext.getString(R.string.service_social_authority), "health",
                appContext.getString(R.string.service_social_badge), 0,
                appContext.getString(R.string.service_days_5_10), "1 año",
                "NIN, composición familiar y comprobante de ingresos.",
                "Notificación y credencial digital", healthId, true);
        Service parking = service("Permiso de estacionamiento residencial",
                "Acceso a la zona del Sector 4 para residentes con vehículo registrado.",
                "Autoridad de Transporte", "transport", null, 25,
                "24 horas", "1 año",
                "Título o registro vehicular, comprobante de residencia e historial municipal limpio.",
                "Despacho digital", transportId, true);
        Service taxes = service("Declaración de impuestos nacionales",
                "Presenta tu declaración prellenada del ejercicio fiscal y revisa créditos aplicables.",
                "Autoridad de Ingresos", "taxes", "Ejercicio 2024-25", 0,
                "8 minutos", "Anual",
                "Formulario prellenado y comprobantes de ingresos.",
                "Portal digital", revenueId, true);
        Service healthCard = service("Tarjeta de salud y vacunas",
                "Consulta beneficios, vacunas y citas de atención primaria.",
                "Servicio Nacional de Salud", "health", null, 0,
                "5 días", "2 años",
                "NIN y comprobante de residencia.", "Billetera digital", healthId, true);
        Service appointments = service("Citas consulares y de registro",
                "Reserva citas para trámites consulares, registro civil y atención presencial.",
                "Oficina de Atención Ciudadana", "civil", null, 0,
                "48 horas", "Cita única",
                "NIN y motivo de la cita.", "Confirmación digital", civilId, true);
        Service housingService = service(appContext.getString(R.string.service_housing_title),
                appContext.getString(R.string.service_housing_desc),
                appContext.getString(R.string.service_housing_authority), "housing", null, 20,
                "3 a 5 días", "Vigencia permanente",
                "Escritura o título de propiedad, NIN y comprobante de domicilio.",
                "PDF firmado o recogida en oficina", housingId, true);
        int passportId = (int) database.serviceDao().insert(passport);
        int driverId = (int) database.serviceDao().insert(driver);
        int civilRecordsId = (int) database.serviceDao().insert(civilRecords);
        int businessId = (int) database.serviceDao().insert(business);
        int socialId = (int) database.serviceDao().insert(social);
        int parkingId = (int) database.serviceDao().insert(parking);
        int taxesId = (int) database.serviceDao().insert(taxes);
        int healthCardId = (int) database.serviceDao().insert(healthCard);
        int appointmentsId = (int) database.serviceDao().insert(appointments);
        int housingServiceId = (int) database.serviceDao().insert(housingService);

        Request passportRequest = request(elenaId, passportId, civilId, RequestStatus.IN_PROGRESS,
                "#REQ-88319", 45, "Mensajería segura", now - 6L * 24 * 60 * 60 * 1000,
                now - 2L * 60 * 60 * 1000, "alta", "Agente #108",
                "Biometría coincidente con el registro civil.");
        Request parkingRequest = request(elenaId, parkingId, transportId, RequestStatus.ACTION,
                "#REQ-90142", 25, "Despacho digital", now - 3L * 24 * 60 * 60 * 1000,
                now - 24L * 60 * 60 * 1000, "urgente", "Agente #412",
                "Falta el comprobante de arrendamiento del vehículo.");
        Request vitalRequest = request(elenaId, civilRecordsId, civilId, RequestStatus.COMPLETED,
                "#REQ-77402", 0, "PDF firmado", now - 12L * 24 * 60 * 60 * 1000,
                now - 9L * 24 * 60 * 60 * 1000, "normal", "Agente #229",
                "Sello de archivo autenticado.");
        Request taxRequest = request(elenaId, taxesId, revenueId, RequestStatus.PENDING,
                "#REQ-89410", 0, "Portal digital", now - 8L * 24 * 60 * 60 * 1000,
                now - 7L * 24 * 60 * 60 * 1000, "normal", "Agente #412",
                "Documentación inicial recibida. Conciliación automática programada.");
        Request marcusParking = request(marcusId, parkingId, transportId, RequestStatus.ACTION,
                "#REQ-90518", 25, "Despacho digital", now - 2L * 24 * 60 * 60 * 1000,
                now - 6L * 60 * 60 * 1000, "alta", "Agente #412",
                "Falta el contrato de arrendamiento vigente.");
        Request aishaBusiness = request(aishaId, businessId, commerceId, RequestStatus.PENDING,
                "#REQ-90211", 80, "Resolución digital", now - 1L * 24 * 60 * 60 * 1000,
                now - 4L * 60 * 60 * 1000, "alta", "Agente #229",
                "Verificación manual del sello fiscal en curso.");
        Request davidVital = request(davidId, civilRecordsId, civilId, RequestStatus.COMPLETED,
                "#REQ-90334", 0, "PDF firmado", now - 5L * 24 * 60 * 60 * 1000,
                now - 2L * 24 * 60 * 60 * 1000, "normal", "Agente #412",
                "Registro emitido y sellado digitalmente.");
        database.requestDao().insert(passportRequest);
        database.requestDao().insert(parkingRequest);
        database.requestDao().insert(vitalRequest);
        database.requestDao().insert(taxRequest);
        database.requestDao().insert(marcusParking);
        database.requestDao().insert(aishaBusiness);
        database.requestDao().insert(davidVital);

        Document nid = document("Tarjeta de identidad ciudadana", "identity",
                "Elena Vargas", "NID-8409-771", "08 nov 2019", "08 nov 2032", true,
                "application/pdf", 0, now - 400L * 24 * 60 * 60 * 1000);
        Document license = document("Licencia de conducir", "license",
                "Elena Vargas", "DL-9938210-C", "14 oct 2021", "14 oct 2028", true,
                "application/pdf", 0, now - 380L * 24 * 60 * 60 * 1000);
        Document medicare = document("Tarjeta de Medicare Universal", "health",
                "Elena Vargas", "HC-440219", "01 ene 2024", "Sin vencimiento", true,
                "application/pdf", 0, now - 200L * 24 * 60 * 60 * 1000);
        int nidId = (int) database.documentDao().insert(nid);
        int licenseId = (int) database.documentDao().insert(license);
        int medicareId = (int) database.documentDao().insert(medicare);
        linkDocument(elenaId, nidId);
        linkDocument(elenaId, licenseId);
        linkDocument(elenaId, medicareId);

        seedNotification(elenaId, "urgent", appContext.getString(R.string.notifications_tax_deadline),
                appContext.getString(R.string.notifications_tax_body), "Revisar y declarar",
                now - 10L * 60 * 1000, "request", taxRequest.id);
        seedNotification(elenaId, "updates", appContext.getString(R.string.notifications_passport_title),
                appContext.getString(R.string.notifications_passport_body), "Seguir paso 3 de 4",
                now - 2L * 60 * 60 * 1000, "request", passportRequest.id);
        seedNotification(elenaId, "legal", appContext.getString(R.string.notifications_license_approved),
                appContext.getString(R.string.notifications_license_body), "Ver en la billetera",
                now - 24L * 60 * 60 * 1000 - 3L * 60 * 60 * 1000, "document", licenseId);
        seedNotification(elenaId, "legal", appContext.getString(R.string.notifications_login),
                appContext.getString(R.string.notifications_login_body), "Verificar dispositivo",
                now - 24L * 60 * 60 * 1000 - 5L * 60 * 60 * 1000, "none", 0);
        seedNotification(elenaId, "updates", appContext.getString(R.string.notifications_water),
                appContext.getString(R.string.notifications_water_body), null,
                now - 3L * 24 * 60 * 60 * 1000, "none", 0);
        seedNotification(elenaId, "updates", appContext.getString(R.string.notifications_voter),
                appContext.getString(R.string.notifications_voter_body), null,
                now - 5L * 24 * 60 * 60 * 1000, "none", 0);
        seedNotification(elenaId, "updates", appContext.getString(R.string.notifications_tax_receipt_title),
                appContext.getString(R.string.notifications_tax_receipt_body), "Ver recibo",
                now - 7L * 24 * 60 * 60 * 1000, "document", medicareId);
        seedNotification(elenaId, "urgent", appContext.getString(R.string.notifications_vehicle_title),
                appContext.getString(R.string.notifications_vehicle_body), "Renovar ahora",
                now - 9L * 24 * 60 * 60 * 1000, "none", 0);

        Message welcome = new Message();
        welcome.role = "assistant";
        welcome.message = appContext.getString(R.string.assistant_welcome);
        welcome.createdAt = now - 60L * 60 * 1000;
        int welcomeId = (int) database.messageDao().insert(welcome);
        MessageUser welcomeLink = new MessageUser();
        welcomeLink.userId = elenaId;
        welcomeLink.messageId = welcomeId;
        database.messageUserDao().insert(welcomeLink);

        database.auditLogDao().insert(audit(elenaId, "login", "user", elenaId,
                "Inicio de sesión biométrico verificado."));
        database.auditLogDao().insert(audit(elenaId, "request_create", "request", passportRequest.id,
                "#REQ-88319 · Renovación de pasaporte."));
        database.auditLogDao().insert(audit(elenaId, "document_view", "document", nidId,
                "Credencial NID presentada para verificación."));
        database.auditLogDao().insert(audit(elenaId, "document_view", "document", licenseId,
                "Credencial de conducir presentada para verificación."));
        database.auditLogDao().insert(audit(elenaId, "document_view", "document", medicareId,
                "Credencial de salud presentada para verificación."));

        ServiceUser bookmark = new ServiceUser();
        bookmark.userId = elenaId;
        bookmark.serviceId = passportId;
        database.serviceUserDao().insert(bookmark);

        // Keep the compiler honest about unused identifiers in the seed data.
        if (driverId == healthCardId && socialId == appointmentsId) {
            Log.d(TAG, "Catálogo de servicios preparado.");
        }
    }

    private Profile profile(String fullName, String nin, String phone, String address, String code,
                            String dob, String district, String marital, String polling, String tier,
                            String memberSince, String initials, boolean verified) {
        Profile profile = new Profile();
        profile.fullName = fullName;
        profile.dni = nin;
        profile.phone = phone;
        profile.adress = address;
        profile.code = code;
        profile.dateOfBirth = dob;
        profile.city = "Capital Cívica";
        profile.district = district;
        profile.maritalStatus = marital;
        profile.pollingPlace = polling;
        profile.tier = tier;
        profile.memberSince = memberSince;
        profile.avatarInitials = initials;
        profile.verified = verified;
        profile.biometricEnabled = true;
        profile.twoFactorEnabled = true;
        profile.emergencySms = true;
        profile.taxPush = true;
        profile.healthAlerts = false;
        profile.language = "Español (oficial)";
        return profile;
    }

    private Department department(String name) {
        Department department = new Department();
        department.name = name;
        return department;
    }

    private Role role(String name) {
        Role role = new Role();
        role.name = name;
        return role;
    }

    private User user(String name, String email, String password, String nin, String role,
                      int profileId, long createdAt) {
        User user = new User();
        user.name = name;
        user.email = email;
        user.password = PasswordHasher.hash(password);
        user.nin = nin;
        user.role = role;
        user.profileId = profileId;
        user.createdAt = createdAt;
        return user;
    }

    private Service service(String name, String description, String authority, String category,
                            String badge, double fee, String time, String validity,
                            String requiredDocuments, String deliveryOptions, int departmentId,
                            boolean active) {
        Service service = new Service();
        service.name = name;
        service.description = description;
        service.authority = authority;
        service.category = category;
        service.badge = badge;
        service.fee = fee;
        service.time = time;
        service.validity = validity;
        service.requiredDocuments = requiredDocuments;
        service.deliveryOptions = deliveryOptions;
        service.departmentId = departmentId;
        service.active = active;
        return service;
    }

    private Request request(int userId, int serviceId, int departmentId, String status,
                            String reference, double fee, String delivery, long createdAt,
                            long updatedAt, String priority, String officer, String notes) {
        Request request = new Request();
        request.userId = userId;
        request.servicesId = serviceId;
        request.departmentId = departmentId;
        request.status = status;
        request.reference = reference;
        request.fee = fee;
        request.deliveryMethod = delivery;
        request.createdAt = createdAt;
        request.updatedAt = updatedAt;
        request.priority = priority;
        request.assignedOfficer = officer;
        request.notes = notes;
        request.time = "24 horas";
        return request;
    }

    private Document document(String title, String type, String holder, String number,
                              String issued, String expires, boolean verified, String mime,
                              long size, long createdAt) {
        Document document = new Document();
        document.title = title;
        document.type = type;
        document.holderName = holder;
        document.documentNumber = number;
        document.issuedAt = issued;
        document.expiresAt = expires;
        document.verified = verified;
        document.mimeType = mime;
        document.fileSize = size;
        document.createdAt = createdAt;
        document.path = "";
        return document;
    }

    private void linkDocument(int userId, int documentId) {
        DocumentUser link = new DocumentUser();
        link.userId = userId;
        link.documentId = documentId;
        database.documentUserDao().insert(link);
    }

    private void seedNotification(int userId, String category, String title, String body,
                                  String action, long createdAt, String relatedType, int relatedId) {
        Notification notification = new Notification();
        notification.category = category;
        notification.title = title;
        notification.body = body;
        notification.timeLabel = Formatters.relative(createdAt);
        notification.actionLabel = action;
        notification.read = false;
        notification.dismissed = false;
        notification.createdAt = createdAt;
        notification.relatedType = relatedType;
        notification.relatedId = relatedId;
        int notificationId = (int) database.notificationDao().insert(notification);
        NotificationUser link = new NotificationUser();
        link.userId = userId;
        link.notificationId = notificationId;
        database.notificationUserDao().insert(link);
    }
}
