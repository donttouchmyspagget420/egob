package deez.nuts.egob.data;

import java.util.Locale;

/** Metadata for one SQLite column shown in the admin CRUD editor. */
public final class DbColumnInfo {
    public final String name;
    public final String type;
    public final boolean notNull;
    public final String defaultValue;
    public final boolean primaryKey;

    public DbColumnInfo(String name, String type, boolean notNull, String defaultValue,
                        boolean primaryKey) {
        this.name = name;
        this.type = type == null ? "" : type;
        this.notNull = notNull;
        this.defaultValue = defaultValue;
        this.primaryKey = primaryKey;
    }

    public boolean isNumeric() {
        String upper = type.toUpperCase(Locale.US);
        return upper.contains("INT") || upper.contains("REAL") || upper.contains("FLOA")
                || upper.contains("DOUB") || upper.contains("NUM");
    }

    public boolean isBooleanLike() {
        String lower = name.toLowerCase(Locale.US);
        return lower.endsWith("_enabled") || "verified".equals(lower) || "active".equals(lower)
                || "read".equals(lower) || "dismissed".equals(lower);
    }

    public String label() {
        switch (name) {
            case "id":
                return "ID";
            case "name":
                return "Nombre";
            case "full_name":
                return "Nombre completo";
            case "email":
                return "Correo";
            case "password":
                return "Clave (texto plano o hash)";
            case "nin":
                return "NIN";
            case "dni":
                return "NIN / DNI";
            case "role":
                return "Rol";
            case "profile_id":
                return "ID de perfil";
            case "user_id":
                return "ID de usuario";
            case "department_id":
                return "ID de departamento";
            case "services_id":
                return "ID de servicio";
            case "service_id":
                return "ID de servicio";
            case "request_id":
                return "ID de solicitud";
            case "document_id":
                return "ID de documento";
            case "notification_id":
                return "ID de notificación";
            case "message_id":
                return "ID de mensaje";
            case "role_id":
                return "ID de rol";
            case "created_at":
                return "Creado";
            case "updated_at":
                return "Actualizado";
            case "issued_at":
                return "Emitido";
            case "expires_at":
                return "Vence";
            case "date_of_birth":
                return "Fecha de nacimiento";
            case "marital_status":
                return "Estado civil";
            case "polling_place":
                return "Centro de votación";
            case "member_since":
                return "Miembro desde";
            case "avatar_initials":
                return "Iniciales";
            case "biometric_enabled":
                return "Biometría habilitada";
            case "qr_enabled":
                return "QR habilitado";
            case "access_key_enabled":
                return "Llave de acceso habilitada";
            case "two_factor_enabled":
                return "Doble factor habilitado";
            case "emergency_sms":
                return "SMS de emergencia";
            case "tax_push":
                return "Alertas fiscales";
            case "health_alerts":
                return "Alertas de salud";
            case "document_number":
                return "Número de documento";
            case "holder_name":
                return "Titular";
            case "mime_type":
                return "Tipo MIME";
            case "file_size":
                return "Tamaño de archivo";
            case "delivery_method":
                return "Método de entrega";
            case "assigned_officer":
                return "Oficial asignado";
            case "related_type":
                return "Tipo relacionado";
            case "related_id":
                return "ID relacionado";
            case "action_label":
                return "Etiqueta de acción";
            case "time_label":
                return "Etiqueta de tiempo";
            case "entity_type":
                return "Tipo de entidad";
            case "entity_id":
                return "ID de entidad";
            case "actor_user_id":
                return "ID del actor";
            case "required_documents":
                return "Documentos requeridos";
            case "delivery_options":
                return "Opciones de entrega";
            default:
                return humanize(name);
        }
    }

    private static String humanize(String value) {
        String text = value == null ? "" : value.replace('_', ' ').trim();
        if (text.isEmpty()) {
            return "Campo";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
