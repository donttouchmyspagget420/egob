package deez.nuts.egob.data;

import deez.nuts.egob.database.entities.Profile;
import deez.nuts.egob.database.entities.User;

/** User plus profile security flags shown in the admin access screen. */
public final class AdminUserAccess {
    public final User user;
    public final Profile profile;

    public AdminUserAccess(User user, Profile profile) {
        this.user = user;
        this.profile = profile;
    }
}
