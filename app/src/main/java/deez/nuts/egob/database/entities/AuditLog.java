package deez.nuts.egob.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "audit_log")
public class AuditLog {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "actor_user_id")
    public int actorUserId;

    public String action;

    @ColumnInfo(name = "entity_type")
    public String entityType;

    @ColumnInfo(name = "entity_id")
    public int entityId;

    public String details;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
