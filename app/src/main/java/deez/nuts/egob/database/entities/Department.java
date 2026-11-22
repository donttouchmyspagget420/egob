package deez.nuts.egob.database.entities;


import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "department")
public class Department {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
}