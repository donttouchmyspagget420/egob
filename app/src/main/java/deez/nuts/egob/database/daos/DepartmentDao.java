package deez.nuts.egob.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import deez.nuts.egob.database.entities.Department;
import java.util.List;

@Dao
public interface DepartmentDao {
    @Insert
    long insert(Department department);

    @Update
    void update(Department department);

    @Delete
    void delete(Department department);

    @Query("SELECT * FROM department")
    List<Department> getAll();

    @Query("SELECT * FROM department WHERE id = :id")
    Department getById(int id);
}