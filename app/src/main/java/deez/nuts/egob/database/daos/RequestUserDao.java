package deez.nuts.egob.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import deez.nuts.egob.database.entities.Request;
import deez.nuts.egob.database.entities.RequestUser;
import java.util.List;

@Dao
public interface RequestUserDao {
    @Insert
    long insert(RequestUser requestUser);

    @Delete
    void delete(RequestUser requestUser);

    @Query("SELECT * FROM request_user WHERE user_id = :userId")
    List<RequestUser> getByUserId(int userId);

    @Query("SELECT requests.* FROM requests INNER JOIN request_user ON requests.id = request_user.request_id WHERE request_user.user_id = :userId")
    List<Request> getRequestsForUser(int userId);
}
