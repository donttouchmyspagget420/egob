package deez.nuts.egob.data;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import deez.nuts.egob.database.entities.Service;
import deez.nuts.egob.database.pojos.RequestWithService;
import deez.nuts.egob.util.Formatters;
import deez.nuts.egob.util.RequestStatus;

/**
 * Deterministic local civic assistant. It answers from the persisted service catalogue and
 * the citizen's own request history, so it works without a network or external AI key.
 */
public final class AssistantEngine {

    public static final class Reply {
        public final String text;
        public final int serviceId;

        Reply(String text, int serviceId) {
            this.text = text;
            this.serviceId = serviceId;
        }
    }

    public Reply reply(String rawInput, List<Service> services, List<RequestWithService> requests) {
        String input = normalize(rawInput);
        if (input.isEmpty()) {
            return new Reply("Escribe una consulta sobre servicios, solicitudes o documentos.", 0);
        }

        if (containsAny(input, "hola", "buenos dias", "buenas tardes", "buenas noches", "hey")) {
            return new Reply("Hola. Soy eGobBot. Puedo consultar el catálogo de servicios, "
                    + "explicarte los requisitos de un trámite o revisar el estado de tus solicitudes.", 0);
        }

        if (containsAny(input, "solicitud", "solicitudes", "estado", "tramite", "expediente", "seguimiento")) {
            return new Reply(requestSummary(requests), 0);
        }

        if (containsAny(input, "documento", "documentos", "credencial", "billetera", "wallet")) {
            return new Reply("Tus credenciales están en la pestaña Documentos. Allí puedes abrir el QR "
                    + "firmado, descargar una copia en PDF o añadir un archivo a tu bóveda cifrada.", 0);
        }

        if (containsAny(input, "perfil", "domicilio", "direccion", "correo", "telefono", "datos personales")) {
            return new Reply("Puedes revisar y actualizar tus datos desde Perfil. Los cambios de domicilio "
                    + "generan una solicitud de corrección en el Registro Civil y quedan auditados.", 0);
        }

        Service match = findService(input, services);
        if (match != null) {
            return new Reply(serviceSummary(match), match.id);
        }

        return new Reply("No encontré un trámite exacto para esa consulta. Prueba con «pasaporte», "
                + "«licencia de conducir», «impuestos», «estacionamiento», «salud» o «solicitudes».", 0);
    }

    private String requestSummary(List<RequestWithService> requests) {
        if (requests == null || requests.isEmpty()) {
            return "No tienes solicitudes registradas. Abre Servicios para iniciar un trámite.";
        }
        StringBuilder builder = new StringBuilder("Tienes ")
                .append(requests.size())
                .append(requests.size() == 1 ? " solicitud registrada:\n" : " solicitudes registradas:\n");
        int shown = 0;
        for (RequestWithService row : requests) {
            if (row.request == null || shown == 3) {
                break;
            }
            shown++;
            builder.append("• ").append(row.request.reference)
                    .append(" — ").append(row.serviceName)
                    .append(": ").append(RequestStatus.label(row.request.status));
            if (row.request.status.equals(RequestStatus.ACTION)) {
                builder.append(" (faltan documentos)");
            }
            builder.append('\n');
        }
        builder.append("Puedes abrir la pestaña Solicitudes para ver el detalle completo.");
        return builder.toString();
    }

    private String serviceSummary(Service service) {
        StringBuilder builder = new StringBuilder();
        builder.append(service.name).append(" — ").append(service.authority).append(".\n");
        builder.append("Tarifa: ").append(Formatters.currency(service.fee))
                .append(" · Plazo: ").append(service.time).append(".\n");
        if (service.requiredDocuments != null && !service.requiredDocuments.isEmpty()) {
            builder.append("Documentos: ").append(service.requiredDocuments).append(".\n");
        }
        builder.append("Puedes iniciar la solicitud desde Servicios o desde el detalle del trámite.");
        return builder.toString();
    }

    private Service findService(String input, List<Service> services) {
        if (services == null) {
            return null;
        }
        Service best = null;
        int bestScore = 0;
        for (Service service : services) {
            String name = normalize(service.name + " " + service.category);
            String haystack = normalize(service.name + " " + service.category + " "
                    + service.description + " " + service.authority);
            int score = 0;
            for (String token : input.split("\\s+")) {
                if (token.length() < 4) {
                    continue;
                }
                if (name.contains(token)) {
                    score += 3;
                } else if (haystack.contains(token)) {
                    score += 1;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = service;
            }
        }
        if (best != null) {
            return best;
        }
        if (containsAny(input, "impuesto", "declaracion", "renta", "fiscal")) {
            return byCategory(services, "taxes");
        }
        if (containsAny(input, "salud", "medico", "seguro", "familia")) {
            return byCategory(services, "health");
        }
        if (containsAny(input, "carro", "vehiculo", "conducir", "licencia")) {
            return byCategory(services, "transport");
        }
        if (containsAny(input, "pasaporte", "nacimiento", "acta", "identidad", "cita")) {
            return byCategory(services, "civil");
        }
        return null;
    }

    private Service byCategory(List<Service> services, String category) {
        for (Service service : services) {
            if (category.equals(service.category)) {
                return service;
            }
        }
        return null;
    }

    private boolean containsAny(String input, String... values) {
        for (String value : values) {
            if (input.contains(value)) {
                return true;
            }
        }
        return false;
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value.toLowerCase(Locale.getDefault()), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").replaceAll("[^a-z0-9\\s]", " ").trim();
    }
}
