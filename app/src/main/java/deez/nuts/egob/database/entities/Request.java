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
    public String reference;
    public double fee;

    @ColumnInfo(name = "delivery_method")
    public String deliveryMethod;

    @ColumnInfo(name = "department_id")
    public int departmentId;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public String priority;

    @ColumnInfo(name = "assigned_officer")
    public String assignedOfficer;

    public String notes;
}
