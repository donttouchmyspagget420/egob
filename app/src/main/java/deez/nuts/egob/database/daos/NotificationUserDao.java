package deez.nuts.egob.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import deez.nuts.egob.database.entities.Notification;
import deez.nuts.egob.database.entities.NotificationUser;
import java.util.List;

@Dao
public interface NotificationUserDao {
    @Insert
    long insert(NotificationUser notificationUser);

    @Delete
    void delete(NotificationUser notificationUser);

    @Query("SELECT * FROM notification_user WHERE user_id = :userId")
    List<NotificationUser> getByUserId(int userId);

    @Query("SELECT notifications.* FROM notifications INNER JOIN notification_user ON notifications.id = notification_user.notification_id WHERE notification_user.user_id = :userId")
    List<Notification> getNotificationsForUser(int userId);
}