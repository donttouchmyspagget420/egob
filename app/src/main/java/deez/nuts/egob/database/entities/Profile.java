package deez.nuts.egob.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "profiles")
public class Profile {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String dni;
    public String phone;
    public String adress; // Kept spelling from your schema
    public String code;
}
