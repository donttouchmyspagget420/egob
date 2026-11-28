package deez.nuts.egob.data;

import java.util.LinkedHashMap;
import java.util.Map;

/** One raw row from a local table, keyed by column name. */
public final class DbRow {
    private static final String[] TITLE_COLUMNS = {
            "name", "full_name", "title", "reference", "email", "action", "text",
            "message", "path", "dni", "nin"
    };

    public final String primaryKeyColumn;
    public final String primaryKeyValue;
    public final LinkedHashMap<String, String> values;

    public DbRow(String primaryKeyColumn, String primaryKeyValue,
                 LinkedHashMap<String, String> values) {
        this.primaryKeyColumn = primaryKeyColumn;
        this.primaryKeyValue = primaryKeyValue;
        this.values = values;
    }

    public String title() {
        for (String column : TITLE_COLUMNS) {
            String value = values.get(column);
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        if (primaryKeyValue != null) {
            return "#" + primaryKeyValue;
        }
        return "Fila";
    }

    public String subtitle() {
        StringBuilder builder = new StringBuilder();
        if (primaryKeyColumn != null && primaryKeyValue != null) {
            builder.append(primaryKeyColumn).append(": ").append(primaryKeyValue);
        }
        int added = 0;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (added >= 2) {
                break;
            }
            if (entry.getKey().equals(primaryKeyColumn)) {
                continue;
            }
            String value = entry.getValue();
            if (value == null || value.trim().isEmpty() || value.equals(title())) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" · ");
            }
            builder.append(entry.getKey()).append(": ").append(shorten(value));
            added++;
        }
        return builder.toString();
    }

    private static String shorten(String value) {
        String clean = value.replace('\n', ' ').trim();
        return clean.length() <= 42 ? clean : clean.substring(0, 39) + "…";
    }
}
