package deez.nuts.egob.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "services",
        foreignKeys = @ForeignKey(
                entity = Department.class,
                parentColumns = "id",
                childColumns = "department_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("department_id")}
)
public class Service {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String description;
    public double fee;
    public String time;

    @ColumnInfo(name = "department_id")
    public int departmentId;
}