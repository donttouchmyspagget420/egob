package deez.nuts.egob.data;

import java.util.Collections;
import java.util.List;

/** Columns and rows for the admin table browser. */
public final class AdminTableData {
    public final String table;
    public final String primaryKey;
    public final List<DbColumnInfo> columns;
    public final List<DbRow> rows;

    public AdminTableData(String table, String primaryKey, List<DbColumnInfo> columns,
                          List<DbRow> rows) {
        this.table = table;
        this.primaryKey = primaryKey;
        this.columns = columns == null ? Collections.emptyList() : columns;
        this.rows = rows == null ? Collections.emptyList() : rows;
    }
}
