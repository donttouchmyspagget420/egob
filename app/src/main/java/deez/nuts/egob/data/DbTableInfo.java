package deez.nuts.egob.data;

/** A local Room/SQLite table and its current row count. */
public final class DbTableInfo {
    public final String name;
    public final int rowCount;

    public DbTableInfo(String name, int rowCount) {
        this.name = name;
        this.rowCount = rowCount;
    }
}
