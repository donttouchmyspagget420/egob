package deez.nuts.egob.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import deez.nuts.egob.database.entities.Role;
import java.util.List;

@Dao
public interface RoleDao {
    @Insert
    long insert(Role role);

    @Update
    void update(Role role);

    @Delete
    void delete(Role role);

    @Query("SELECT * FROM role")
    List<Role> getAll();

    @Query("SELECT * FROM role WHERE id = :id")
    Role getById(int id);
}