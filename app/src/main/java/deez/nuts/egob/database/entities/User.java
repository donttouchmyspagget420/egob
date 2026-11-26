package deez.nuts.egob.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "users",
        foreignKeys = @ForeignKey(
                entity = Profile.class,
                parentColumns = "id",
                childColumns = "profile_id",
                onDelete = ForeignKey.SET_NULL
        ),
        indices = {
                @Index("profile_id"),
                @Index(value = "email", unique = true),
                @Index(value = "nin", unique = true)
        }
)
public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String email;
    public String password;
    public String nin;
    public String role;

    @ColumnInfo(name = "profile_id")
    public Integer profileId;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
