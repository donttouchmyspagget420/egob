package deez.nuts.egob.ui;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public class ServiceItem {

    @DrawableRes
    public final int iconRes;
    @NonNull
    public final String category;
    @NonNull
    public final String authority;
    @NonNull
    public final String title;
    @NonNull
    public final String description;
    @NonNull
    public final String time;
    @NonNull
    public final String price;
    @NonNull
    public final String action;
    public final String badge;
    public int id;
    public boolean bookmarked;

    public ServiceItem(@DrawableRes int iconRes, @NonNull String category, @NonNull String authority,
                       @NonNull String title, @NonNull String description, @NonNull String time,
                       @NonNull String price, @NonNull String action, String badge) {
        this.iconRes = iconRes;
        this.category = category;
        this.authority = authority;
        this.title = title;
        this.description = description;
        this.time = time;
        this.price = price;
        this.action = action;
        this.badge = badge;
    }
}
