package deez.nuts.egob.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import deez.nuts.egob.database.entities.Profile;
import java.util.List;

@Dao
public interface ProfileDao {
    @Insert
    long insert(Profile profile);

    @Update
    void update(Profile profile);

    @Delete
    void delete(Profile profile);

    @Query("SELECT * FROM profiles")
    List<Profile> getAll();

    @Query("SELECT * FROM profiles WHERE id = :id")
    Profile getById(int id);
}