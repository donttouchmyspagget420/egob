package deez.nuts.egob.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import deez.nuts.egob.database.entities.Document;
import deez.nuts.egob.database.entities.DocumentUser;

@Dao
public interface DocumentUserDao {
    @Insert
    long insert(DocumentUser documentUser);

    @Delete
    void delete(DocumentUser documentUser);

    @Query("SELECT * FROM document_user WHERE user_id = :userId")
    List<DocumentUser> getByUserId(int userId);

    @Query("SELECT documents.* FROM documents INNER JOIN document_user ON documents.id = document_user.document_id WHERE document_user.user_id = :userId ORDER BY documents.created_at DESC")
    List<Document> getDocumentsForUser(int userId);

    @Query("DELETE FROM document_user WHERE document_id = :documentId")
    void deleteByDocumentId(int documentId);
}
