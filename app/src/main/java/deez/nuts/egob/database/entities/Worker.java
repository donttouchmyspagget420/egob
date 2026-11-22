package deez.nuts.egob.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "worker",
        foreignKeys = {
                @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "user_id", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Department.class, parentColumns = "id", childColumns = "department_id", onDelete = ForeignKey.SET_NULL),
                @ForeignKey(entity = Role.class, parentColumns = "id", childColumns = "role_id", onDelete = ForeignKey.SET_NULL)
        },
        indices = {@Index("user_id"), @Index("department_id"), @Index("role_id")}
)
public class Worker {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "user_id")
    public int userId;

    @ColumnInfo(name = "department_id")
    public Integer departmentId;

    @ColumnInfo(name = "role_id")
    public Integer roleId;
}