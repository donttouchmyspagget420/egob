package deez.nuts.egob.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import deez.nuts.egob.database.entities.Document;
import java.util.List;

@Dao
public interface DocumentDao {
    @Insert
    long insert(Document document);

    @Update
    void update(Document document);

    @Delete
    void delete(Document document);

    @Query("SELECT * FROM documents")
    List<Document> getAll();

    @Query("SELECT * FROM documents WHERE id = :id")
    Document getById(int id);
}
