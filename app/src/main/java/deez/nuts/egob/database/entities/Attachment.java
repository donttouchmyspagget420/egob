package deez.nuts.egob.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "attachments",
        foreignKeys = @ForeignKey(
                entity = Request.class,
                parentColumns = "id",
                childColumns = "request_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("request_id")}
)
public class Attachment {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "request_id")
    public int requestId;

    @ColumnInfo(name = "user_id")
    public int userId;

    public String name;
    public String path;

    @ColumnInfo(name = "mime_type")
    public String mimeType;

    @ColumnInfo(name = "file_size")
    public long fileSize;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
