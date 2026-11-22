package deez.nuts.egob.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import deez.nuts.egob.database.entities.Request;
import java.util.List;

@Dao
public interface RequestDao {
    @Insert
    long insert(Request request);

    @Update
    void update(Request request);

    @Delete
    void delete(Request request);

    @Query("SELECT * FROM requests")
    List<Request> getAll();

    @Query("SELECT * FROM requests WHERE id = :id")
    Request getById(int id);

    @Query("SELECT * FROM requests WHERE user_id = :userId")
    List<Request> getByUser(int userId);
}
