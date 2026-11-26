package deez.nuts.egob.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import deez.nuts.egob.database.entities.Notification;
import deez.nuts.egob.database.entities.NotificationUser;

@Dao
public interface NotificationUserDao {
    @Insert
    long insert(NotificationUser notificationUser);

    @Delete
    void delete(NotificationUser notificationUser);

    @Query("SELECT * FROM notification_user WHERE user_id = :userId")
    List<NotificationUser> getByUserId(int userId);

    @Query("SELECT notifications.* FROM notifications INNER JOIN notification_user "
            + "ON notifications.id = notification_user.notification_id "
            + "WHERE notification_user.user_id = :userId AND notifications.dismissed = 0 "
            + "ORDER BY notifications.created_at DESC")
    List<Notification> getActiveForUser(int userId);

    @Query("SELECT notifications.* FROM notifications INNER JOIN notification_user "
            + "ON notifications.id = notification_user.notification_id "
            + "WHERE notification_user.user_id = :userId ORDER BY notifications.created_at DESC")
    List<Notification> getNotificationsForUser(int userId);

    @Query("UPDATE notifications SET read = 1 WHERE id IN "
            + "(SELECT notification_id FROM notification_user WHERE user_id = :userId)")
    void markAllRead(int userId);

    @Query("UPDATE notifications SET read = 1 WHERE id = :notificationId")
    void markRead(int notificationId);

    @Query("UPDATE notifications SET dismissed = 1 WHERE id = :notificationId")
    void dismiss(int notificationId);

    @Query("SELECT COUNT(*) FROM notifications INNER JOIN notification_user "
            + "ON notifications.id = notification_user.notification_id "
            + "WHERE notification_user.user_id = :userId AND notifications.read = 0 "
            + "AND notifications.dismissed = 0")
    int countUnread(int userId);

    @Query("DELETE FROM notification_user WHERE notification_id = :notificationId")
    void deleteLink(int notificationId);
}
