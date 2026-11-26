package deez.nuts.egob.ui;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public class NotificationItem {

    @NonNull
    public final String categoryKey;
    @DrawableRes
    public final int iconRes;
    @DrawableRes
    public final int iconBackgroundRes;
    @ColorRes
    public final int accentColorRes;
    @NonNull
    public final String category;
    @NonNull
    public final String time;
    @NonNull
    public final String title;
    @NonNull
    public final String body;

    public String timeGroup;
    public String chipPrimary;
    public String chipSecondary;
    public String action;
    public String footer;
    public boolean dismissible;
    public int notificationId;
    public String relatedType;
    public int relatedId;
    public boolean read;

    public NotificationItem(@NonNull String categoryKey, @DrawableRes int iconRes,
                            @DrawableRes int iconBackgroundRes, @ColorRes int accentColorRes,
                            @NonNull String category, @NonNull String time,
                            @NonNull String title, @NonNull String body) {
        this.categoryKey = categoryKey;
        this.iconRes = iconRes;
        this.iconBackgroundRes = iconBackgroundRes;
        this.accentColorRes = accentColorRes;
        this.category = category;
        this.time = time;
        this.title = title;
        this.body = body;
    }
}
