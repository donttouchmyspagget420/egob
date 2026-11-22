package deez.nuts.egob.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import deez.nuts.egob.database.entities.Worker;
import java.util.List;

@Dao
public interface WorkerDao {
    @Insert
    long insert(Worker worker);

    @Update
    void update(Worker worker);

    @Delete
    void delete(Worker worker);

    @Query("SELECT * FROM worker")
    List<Worker> getAll();

    @Query("SELECT * FROM worker WHERE id = :id")
    Worker getById(int id);

    @Query("SELECT * FROM worker WHERE department_id = :departmentId")
    List<Worker> getByDepartment(int departmentId);
}