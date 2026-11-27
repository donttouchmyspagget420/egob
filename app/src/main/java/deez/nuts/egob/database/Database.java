package deez.nuts.egob.database;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;

import deez.nuts.egob.database.daos.AttachmentDao;
import deez.nuts.egob.database.daos.AuditLogDao;
import deez.nuts.egob.database.daos.DepartmentDao;
import deez.nuts.egob.database.daos.DocumentDao;
import deez.nuts.egob.database.daos.DocumentUserDao;
import deez.nuts.egob.database.daos.MessageDao;
import deez.nuts.egob.database.daos.MessageUserDao;
import deez.nuts.egob.database.daos.NotificationDao;
import deez.nuts.egob.database.daos.NotificationUserDao;
import deez.nuts.egob.database.daos.ProfileDao;
import deez.nuts.egob.database.daos.RequestDao;
import deez.nuts.egob.database.daos.RequestUserDao;
import deez.nuts.egob.database.daos.RoleDao;
import deez.nuts.egob.database.daos.ServiceDao;
import deez.nuts.egob.database.daos.ServiceUserDao;
import deez.nuts.egob.database.daos.UserDao;
import deez.nuts.egob.database.daos.WorkerDao;
import deez.nuts.egob.database.entities.Attachment;
import deez.nuts.egob.database.entities.AuditLog;
import deez.nuts.egob.database.entities.Department;
import deez.nuts.egob.database.entities.Document;
import deez.nuts.egob.database.entities.DocumentUser;
import deez.nuts.egob.database.entities.Message;
import deez.nuts.egob.database.entities.MessageUser;
import deez.nuts.egob.database.entities.Notification;
import deez.nuts.egob.database.entities.NotificationUser;
import deez.nuts.egob.database.entities.Profile;
import deez.nuts.egob.database.entities.Request;
import deez.nuts.egob.database.entities.RequestUser;
import deez.nuts.egob.database.entities.Role;
import deez.nuts.egob.database.entities.Service;
import deez.nuts.egob.database.entities.ServiceUser;
import deez.nuts.egob.database.entities.User;
import deez.nuts.egob.database.entities.Worker;

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
                Attachment.class,
                AuditLog.class,
                ServiceUser.class,
                RequestUser.class,
                DocumentUser.class,
                NotificationUser.class,
                MessageUser.class
        },
        version = 3,
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
    public abstract AttachmentDao attachmentDao();
    public abstract AuditLogDao auditLogDao();

    public static Database getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (Database.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            Database.class,
                            "database"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
