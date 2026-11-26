package deez.nuts.egob.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "documents")
public class Document {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String path;
    public String title;
    public String type;

    @ColumnInfo(name = "holder_name")
    public String holderName;

    @ColumnInfo(name = "document_number")
    public String documentNumber;

    @ColumnInfo(name = "issued_at")
    public String issuedAt;

    @ColumnInfo(name = "expires_at")
    public String expiresAt;

    public boolean verified;

    @ColumnInfo(name = "mime_type")
    public String mimeType;

    @ColumnInfo(name = "file_size")
    public long fileSize;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
