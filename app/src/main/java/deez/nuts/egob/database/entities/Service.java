package deez.nuts.egob.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
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
    public String validity;
    public String authority;
    public String category;
    public String badge;

    @ColumnInfo(name = "required_documents")
    public String requiredDocuments;

    @ColumnInfo(name = "delivery_options")
    public String deliveryOptions;

    @ColumnInfo(name = "department_id")
    public int departmentId;

    public boolean active;

    @Ignore
    public boolean bookmarked;
}
