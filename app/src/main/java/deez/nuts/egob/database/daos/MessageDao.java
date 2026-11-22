package deez.nuts.egob.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import deez.nuts.egob.database.entities.Message;
import java.util.List;

@Dao
public interface MessageDao {
    @Insert
    long insert(Message message);

    @Update
    void update(Message message);

    @Delete
    void delete(Message message);

    @Query("SELECT * FROM messages")
    List<Message> getAll();

    @Query("SELECT * FROM messages WHERE id = :id")
    Message getById(int id);
}