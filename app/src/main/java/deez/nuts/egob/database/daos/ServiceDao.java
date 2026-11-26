package deez.nuts.egob.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import deez.nuts.egob.database.entities.Service;

@Dao
public interface ServiceDao {
    @Insert
    long insert(Service service);

    @Update
    void update(Service service);

    @Delete
    void delete(Service service);

    @Query("SELECT * FROM services ORDER BY name")
    List<Service> getAll();

    @Query("SELECT * FROM services WHERE active = 1 ORDER BY name")
    List<Service> getActive();

    @Query("SELECT * FROM services WHERE id = :id")
    Service getById(int id);

    @Query("SELECT * FROM services WHERE name = :name LIMIT 1")
    Service getByName(String name);

    @Query("SELECT * FROM services WHERE category = :category AND active = 1 LIMIT 1")
    Service getByCategory(String category);

    @Query("SELECT * FROM services WHERE department_id = :departmentId")
    List<Service> getByDepartment(int departmentId);

    @Query("SELECT COUNT(*) FROM services")
    int count();
}
