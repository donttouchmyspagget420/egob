package deez.nuts.egob.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import deez.nuts.egob.database.entities.Attachment;

@Dao
public interface AttachmentDao {
    @Insert
    long insert(Attachment attachment);

    @Delete
    void delete(Attachment attachment);

    @Query("SELECT * FROM attachments WHERE request_id = :requestId ORDER BY created_at ASC")
    List<Attachment> getByRequest(int requestId);

    @Query("SELECT COUNT(*) FROM attachments WHERE request_id = :requestId")
    int countByRequest(int requestId);
}
