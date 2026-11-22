package deez.nuts.egob.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import deez.nuts.egob.database.entities.Service;
import java.util.List;

@Dao
public interface ServiceDao {
    @Insert
    long insert(Service service);

    @Update
    void update(Service service);

    @Delete
    void delete(Service service);

    @Query("SELECT * FROM services")
    List<Service> getAll();

    @Query("SELECT * FROM services WHERE id = :id")
    Service getById(int id);

    @Query("SELECT * FROM services WHERE department_id = :departmentId")
    List<Service> getByDepartment(int departmentId);
}