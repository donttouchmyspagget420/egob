package deez.nuts.egob.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "requests",
        foreignKeys = {
                @ForeignKey(entity = Service.class, parentColumns = "id", childColumns = "services_id", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "user_id", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Department.class, parentColumns = "id", childColumns = "department_id", onDelete = ForeignKey.CASCADE)
        },
        indices = {@Index("services_id"), @Index("user_id"), @Index("department_id")}
)
public class Request {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "services_id")
    public int servicesId;

    @ColumnInfo(name = "user_id")
    public int userId;

    public String status;
    public String time;

    @ColumnInfo(name = "department_id")
    public int departmentId;

    @ColumnInfo(name = "created_at")
    public long createdAt; // Stored as Unix timestamp
}