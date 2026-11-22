package deez.nuts.egob.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "role")
public class Role {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
}