package deez.nuts.egob.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import deez.nuts.egob.database.entities.Message;
import deez.nuts.egob.database.entities.MessageUser;
import java.util.List;

@Dao
public interface MessageUserDao {
    @Insert
    long insert(MessageUser messageUser);

    @Delete
    void delete(MessageUser messageUser);

    @Query("SELECT * FROM message_user WHERE user_id = :userId")
    List<MessageUser> getByUserId(int userId);

    @Query("SELECT messages.* FROM messages INNER JOIN message_user ON messages.id = message_user.message_id WHERE message_user.user_id = :userId")
    List<Message> getMessagesForUser(int userId);
}
