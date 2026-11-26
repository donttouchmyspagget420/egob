package deez.nuts.egob.util;

import java.util.Arrays;
import java.util.List;

public final class RequestStatus {

    public static final String PENDING = "pending";
    public static final String ACTION = "action";
    public static final String IN_PROGRESS = "in_progress";
    public static final String COMPLETED = "completed";
    public static final String REJECTED = "rejected";

    private RequestStatus() {
    }

    public static boolean canTransition(String from, String to) {
        if (from == null || to == null || from.equals(to)) {
            return false;
        }
        switch (from) {
            case PENDING:
                return Arrays.asList(ACTION, IN_PROGRESS, COMPLETED, REJECTED).contains(to);
            case ACTION:
                return Arrays.asList(IN_PROGRESS, COMPLETED, REJECTED).contains(to);
            case IN_PROGRESS:
                return Arrays.asList(ACTION, COMPLETED, REJECTED).contains(to);
            case COMPLETED:
            case REJECTED:
            default:
                return false;
        }
    }

    public static boolean isOpen(String status) {
        return PENDING.equals(status) || ACTION.equals(status) || IN_PROGRESS.equals(status);
    }

    public static String label(String status) {
        if (status == null) {
            return "Desconocido";
        }
        switch (status) {
            case PENDING:
                return "Pendiente";
            case ACTION:
                return "Acción requerida";
            case IN_PROGRESS:
                return "En proceso";
            case COMPLETED:
                return "Completada";
            case REJECTED:
                return "Rechazada";
            default:
                return status;
        }
    }

    public static List<String> openStatuses() {
        return Arrays.asList(PENDING, ACTION, IN_PROGRESS);
    }
}
