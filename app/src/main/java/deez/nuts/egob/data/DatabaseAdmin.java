package deez.nuts.egob.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import deez.nuts.egob.database.Database;
import deez.nuts.egob.security.PasswordHasher;

/**
 * Small, deliberately generic SQLite browser used only by the administrator screen.
 * Table and column names are validated before being interpolated into SQL.
 */
public final class DatabaseAdmin {

    private static final int ROW_LIMIT = 200;
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern PASSWORD_HASH = Pattern.compile("\\d+:[^:]+:[^:]+");

    private final Database database;

    public DatabaseAdmin(Database database) {
        this.database = database;
    }

    public List<DbTableInfo> tables() {
        List<DbTableInfo> tables = new ArrayList<>();
        try (Cursor cursor = db().rawQuery(
                "SELECT name FROM sqlite_master WHERE type = 'table' "
                        + "AND name NOT LIKE 'sqlite_%' ORDER BY name", null)) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                if (isSystemTable(name)) {
                    continue;
                }
                tables.add(new DbTableInfo(name, count(name)));
            }
        }
        return tables;
    }

    public AdminTableData tableData(String table) {
        validateTable(table);
        List<DbColumnInfo> columns = columns(table);
        String primaryKey = primaryKey(columns);
        return new AdminTableData(table, primaryKey, columns, rows(table, columns));
    }

    public long insert(String table, Map<String, String> input) {
        validateTable(table);
        List<DbColumnInfo> columns = columns(table);
        ContentValues values = new ContentValues();
        for (DbColumnInfo column : columns) {
            if (column.primaryKey || !input.containsKey(column.name)) {
                continue;
            }
            putValue(values, table, column, input.get(column.name), true);
        }
        return db().insertOrThrow(table, null, values);
    }

    public void update(String table, String primaryKeyColumn, String primaryKeyValue,
                       Map<String, String> input) {
        validateTable(table);
        if (primaryKeyColumn == null || primaryKeyValue == null) {
            throw new IllegalArgumentException("Esta tabla no tiene una clave primaria editable.");
        }
        List<DbColumnInfo> columns = columns(table);
        ContentValues values = new ContentValues();
        for (DbColumnInfo column : columns) {
            if (column.name.equals(primaryKeyColumn) || !input.containsKey(column.name)) {
                continue;
            }
            putValue(values, table, column, input.get(column.name), false);
        }
        int updated = db().update(table, values, quote(primaryKeyColumn) + " = ?",
                new String[]{primaryKeyValue});
        if (updated == 0) {
            throw new IllegalArgumentException("No se encontró la fila seleccionada.");
        }
    }

    public void delete(String table, String primaryKeyColumn, String primaryKeyValue) {
        validateTable(table);
        if (primaryKeyColumn == null || primaryKeyValue == null) {
            throw new IllegalArgumentException("Esta tabla no tiene una clave primaria editable.");
        }
        int deleted = db().delete(table, quote(primaryKeyColumn) + " = ?",
                new String[]{primaryKeyValue});
        if (deleted == 0) {
            throw new IllegalArgumentException("No se encontró la fila seleccionada.");
        }
    }

    private List<DbColumnInfo> columns(String table) {
        validateTable(table);
        List<DbColumnInfo> columns = new ArrayList<>();
        try (Cursor cursor = db().rawQuery("PRAGMA table_info(" + quote(table) + ")", null)) {
            int nameIndex = cursor.getColumnIndexOrThrow("name");
            int typeIndex = cursor.getColumnIndexOrThrow("type");
            int notNullIndex = cursor.getColumnIndexOrThrow("notnull");
            int defaultIndex = cursor.getColumnIndexOrThrow("dflt_value");
            int primaryIndex = cursor.getColumnIndexOrThrow("pk");
            while (cursor.moveToNext()) {
                String defaultValue = cursor.isNull(defaultIndex) ? null : cursor.getString(defaultIndex);
                columns.add(new DbColumnInfo(
                        cursor.getString(nameIndex),
                        cursor.getString(typeIndex),
                        cursor.getInt(notNullIndex) != 0,
                        defaultValue,
                        cursor.getInt(primaryIndex) > 0));
            }
        }
        return columns;
    }

    private List<DbRow> rows(String table, List<DbColumnInfo> columns) {
        String primaryKey = primaryKey(columns);
        String order = primaryKey == null ? null : quote(primaryKey) + " DESC";
        List<DbRow> rows = new ArrayList<>();
        try (Cursor cursor = db().query(table, null, null, null, null, null, order,
                String.valueOf(ROW_LIMIT))) {
            while (cursor.moveToNext()) {
                LinkedHashMap<String, String> values = new LinkedHashMap<>();
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    values.put(cursor.getColumnName(i),
                            cursor.isNull(i) ? null : cursor.getString(i));
                }
                String primaryValue = primaryKey == null ? null : values.get(primaryKey);
                rows.add(new DbRow(primaryKey, primaryValue, values));
            }
        }
        return rows;
    }

    private int count(String table) {
        try (Cursor cursor = db().rawQuery("SELECT COUNT(*) FROM " + quote(table), null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    private static String primaryKey(List<DbColumnInfo> columns) {
        for (DbColumnInfo column : columns) {
            if (column.primaryKey) {
                return column.name;
            }
        }
        return null;
    }

    private void putValue(ContentValues values, String table, DbColumnInfo column,
                          String raw, boolean insert) {
        if (raw == null || raw.trim().isEmpty()) {
            if (insert && column.defaultValue != null) {
                return;
            }
            if (!column.notNull) {
                values.putNull(column.name);
            } else if (column.isNumeric()) {
                values.put(column.name, 0);
            } else {
                values.put(column.name, "");
            }
            return;
        }
        String value = raw;
        if ("users".equals(table) && "password".equals(column.name)
                && !PASSWORD_HASH.matcher(value).matches()) {
            value = PasswordHasher.hash(value);
        }
        if (!column.isNumeric()) {
            values.put(column.name, value);
            return;
        }
        String normalized = value.trim().toLowerCase(Locale.US);
        if ("true".equals(normalized) || "sí".equals(normalized) || "si".equals(normalized)) {
            values.put(column.name, 1);
            return;
        }
        if ("false".equals(normalized) || "no".equals(normalized)) {
            values.put(column.name, 0);
            return;
        }
        try {
            if (column.type.toUpperCase(Locale.US).contains("REAL")
                    || column.type.toUpperCase(Locale.US).contains("FLOA")
                    || column.type.toUpperCase(Locale.US).contains("DOUB")
                    || column.type.toUpperCase(Locale.US).contains("NUM")) {
                values.put(column.name, Double.parseDouble(normalized.replace(',', '.')));
            } else {
                values.put(column.name, Long.parseLong(normalized));
            }
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("El campo \"" + column.label()
                    + "\" debe ser numérico.");
        }
    }

    private static String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static void validateTable(String table) {
        if (table == null || !SAFE_IDENTIFIER.matcher(table).matches() || isSystemTable(table)) {
            throw new IllegalArgumentException("Tabla no válida.");
        }
    }

    private static boolean isSystemTable(String table) {
        return "android_metadata".equals(table)
                || "room_master_table".equals(table)
                || "sqlite_sequence".equals(table);
    }

    private SQLiteDatabase db() {
        return database.getOpenHelper().getWritableDatabase();
    }
}
