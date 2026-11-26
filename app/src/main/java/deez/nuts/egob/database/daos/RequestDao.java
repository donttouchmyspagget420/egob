package deez.nuts.egob.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import deez.nuts.egob.database.entities.Request;
import deez.nuts.egob.database.pojos.RequestWithService;

@Dao
public interface RequestDao {
    @Insert
    long insert(Request request);

    @Update
    void update(Request request);

    @Delete
    void delete(Request request);

    @Query("SELECT * FROM requests ORDER BY created_at DESC")
    List<Request> getAll();

    @Query("SELECT * FROM requests WHERE id = :id")
    Request getById(int id);

    @Query("SELECT * FROM requests WHERE user_id = :userId ORDER BY created_at DESC")
    List<Request> getByUser(int userId);

    @Query("SELECT requests.*, services.name AS service_name, services.authority AS service_authority, "
            + "services.category AS service_category, services.time AS service_time, users.name AS citizen_name "
            + "FROM requests INNER JOIN services ON requests.services_id = services.id "
            + "INNER JOIN users ON requests.user_id = users.id "
            + "WHERE requests.user_id = :userId ORDER BY requests.created_at DESC")
    List<RequestWithService> getByUserWithService(int userId);

    @Query("SELECT requests.*, services.name AS service_name, services.authority AS service_authority, "
            + "services.category AS service_category, services.time AS service_time, users.name AS citizen_name "
            + "FROM requests INNER JOIN services ON requests.services_id = services.id "
            + "INNER JOIN users ON requests.user_id = users.id "
            + "ORDER BY requests.created_at DESC")
    List<RequestWithService> getAllWithService();

    @Query("SELECT requests.*, services.name AS service_name, services.authority AS service_authority, "
            + "services.category AS service_category, services.time AS service_time, users.name AS citizen_name "
            + "FROM requests INNER JOIN services ON requests.services_id = services.id "
            + "INNER JOIN users ON requests.user_id = users.id "
            + "WHERE requests.id = :requestId LIMIT 1")
    RequestWithService getWithServiceById(int requestId);

    @Query("SELECT COUNT(*) FROM requests WHERE status = :status")
    int countByStatus(String status);

    @Query("SELECT COUNT(*) FROM requests WHERE status IN (:statuses)")
    int countByStatuses(List<String> statuses);

    @Query("UPDATE requests SET status = :status, updated_at = :updatedAt, "
            + "assigned_officer = :officer WHERE id = :requestId")
    int updateStatus(int requestId, String status, long updatedAt, String officer);

    @Query("UPDATE requests SET assigned_officer = :officer, updated_at = :updatedAt "
            + "WHERE status IN (:statuses)")
    int reassignOpenRequests(String officer, long updatedAt, List<String> statuses);
}
