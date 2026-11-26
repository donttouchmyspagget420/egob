package deez.nuts.egob.database.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import deez.nuts.egob.database.entities.AuditLog;

@Dao
public interface AuditLogDao {
    @Insert
    long insert(AuditLog log);

    @Query("SELECT * FROM audit_log ORDER BY created_at DESC LIMIT :limit")
    List<AuditLog> getRecent(int limit);

    @Query("SELECT * FROM audit_log WHERE actor_user_id = :userId ORDER BY created_at DESC LIMIT :limit")
    List<AuditLog> getForActor(int userId, int limit);

    @Query("SELECT COUNT(*) FROM audit_log WHERE actor_user_id = :userId")
    int countForActor(int userId);
}
