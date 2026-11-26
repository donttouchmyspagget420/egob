package deez.nuts.egob.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import deez.nuts.egob.database.entities.User;

@Dao
public interface UserDao {
    @Insert
    long insert(User user);

    @Update
    void update(User user);

    @Delete
    void delete(User user);

    @Query("SELECT * FROM users")
    List<User> getAll();

    @Query("SELECT * FROM users WHERE id = :id")
    User getById(int id);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getByEmail(String email);

    @Query("SELECT * FROM users WHERE nin = :nin LIMIT 1")
    User getByNin(String nin);

    @Query("SELECT * FROM users WHERE nin = :identifier OR email = :identifier LIMIT 1")
    User findByIdentifier(String identifier);

    @Query("SELECT COUNT(*) FROM users")
    int count();
}
