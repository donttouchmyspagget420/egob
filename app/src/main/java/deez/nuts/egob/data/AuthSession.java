package deez.nuts.egob.data;

import deez.nuts.egob.database.entities.Profile;
import deez.nuts.egob.database.entities.User;

public class AuthSession {
    public final User user;
    public final Profile profile;

    public AuthSession(User user, Profile profile) {
        this.user = user;
        this.profile = profile;
    }
}
