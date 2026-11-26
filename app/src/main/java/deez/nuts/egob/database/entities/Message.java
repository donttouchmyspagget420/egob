package deez.nuts.egob.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class Message {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String message;
    public String role;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
