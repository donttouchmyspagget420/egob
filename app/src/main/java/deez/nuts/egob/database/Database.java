package deez.nuts.egob.database;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;

import deez.nuts.egob.database.daos.*;
import deez.nuts.egob.database.entities.*;

@androidx.room.Database(
        entities = {
                User.class,
                Profile.class,
                Department.class,
                Role.class,
                Worker.class,
                Service.class,
                Request.class,
                Document.class,
                Notification.class,
                Message.class,
                ServiceUser.class,
                RequestUser.class,
                DocumentUser.class,
                NotificationUser.class,
                MessageUser.class
        },
        version = 1,
        exportSchema = false
)
public abstract class Database extends RoomDatabase {

    private static volatile Database INSTANCE;

    public abstract UserDao userDao();
    public abstract ProfileDao profileDao();
    public abstract DepartmentDao departmentDao();
    public abstract RoleDao roleDao();
    public abstract WorkerDao workerDao();
    public abstract ServiceDao serviceDao();
    public abstract RequestDao requestDao();
    public abstract DocumentDao documentDao();
    public abstract NotificationDao notificationDao();
    public abstract MessageDao messageDao();
    public abstract ServiceUserDao serviceUserDao();
    public abstract RequestUserDao requestUserDao();
    public abstract DocumentUserDao documentUserDao();
    public abstract NotificationUserDao notificationUserDao();
    public abstract MessageUserDao messageUserDao();

    public static Database getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (Database.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            Database.class,
                            "database"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
