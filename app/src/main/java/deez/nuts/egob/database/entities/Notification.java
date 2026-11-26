package deez.nuts.egob.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notifications")
public class Notification {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String text;
    public String category;
    public String title;
    public String body;

    @ColumnInfo(name = "time_label")
    public String timeLabel;

    @ColumnInfo(name = "action_label")
    public String actionLabel;

    public boolean read;
    public boolean dismissed;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "related_type")
    public String relatedType;

    @ColumnInfo(name = "related_id")
    public int relatedId;
}
