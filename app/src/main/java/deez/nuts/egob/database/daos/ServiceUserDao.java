package deez.nuts.egob.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import deez.nuts.egob.database.entities.Service;
import deez.nuts.egob.database.entities.ServiceUser;

@Dao
public interface ServiceUserDao {
    @Insert
    long insert(ServiceUser serviceUser);

    @Delete
    void delete(ServiceUser serviceUser);

    @Query("SELECT * FROM service_user WHERE user_id = :userId")
    List<ServiceUser> getByUserId(int userId);

    @Query("SELECT service_id FROM service_user WHERE user_id = :userId")
    List<Integer> getServiceIds(int userId);

    @Query("SELECT COUNT(*) FROM service_user WHERE user_id = :userId AND service_id = :serviceId")
    int countBookmark(int userId, int serviceId);

    @Query("DELETE FROM service_user WHERE user_id = :userId AND service_id = :serviceId")
    void deleteBookmark(int userId, int serviceId);

    @Query("SELECT services.* FROM services INNER JOIN service_user ON services.id = service_user.service_id WHERE service_user.user_id = :userId")
    List<Service> getServicesForUser(int userId);
}
