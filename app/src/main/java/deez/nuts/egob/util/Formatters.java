package deez.nuts.egob.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Formatters {

    private static final Locale SPANISH = new Locale("es", "ES");

    private Formatters() {
    }

    public static String currency(double value) {
        return String.format(Locale.US, "$%.2f", value);
    }

    public static String date(long timestamp) {
        return new SimpleDateFormat("dd MMM yyyy", SPANISH).format(new Date(timestamp));
    }

    public static String dateTime(long timestamp) {
        return new SimpleDateFormat("dd MMM yyyy • HH:mm", SPANISH).format(new Date(timestamp));
    }

    public static String time(long timestamp) {
        return new SimpleDateFormat("HH:mm", SPANISH).format(new Date(timestamp));
    }

    public static String relative(long timestamp) {
        long delta = System.currentTimeMillis() - timestamp;
        long minutes = delta / (60 * 1000);
        if (minutes < 1) {
            return "Ahora";
        }
        if (minutes < 60) {
            return "Hace " + minutes + " min";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return "Hace " + hours + " h";
        }
        long days = hours / 24;
        if (days == 1) {
            return "Ayer";
        }
        if (days < 7) {
            return "Hace " + days + " días";
        }
        return date(timestamp);
    }

    public static String initials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "EG";
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty() && builder.length() < 2) {
                builder.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return builder.toString();
    }
}
