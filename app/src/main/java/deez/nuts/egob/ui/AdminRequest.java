package deez.nuts.egob.ui;

import androidx.annotation.NonNull;

public class AdminRequest {

    public static final int STATUS_ERROR = 0;
    public static final int STATUS_INFO = 1;
    public static final int STATUS_WARNING = 2;
    public static final int STATUS_SUCCESS = 3;

    @NonNull
    public final String id;
    @NonNull
    public final String citizen;
    @NonNull
    public final String initials;
    @NonNull
    public final String service;
    @NonNull
    public final String submitted;
    @NonNull
    public final String officer;
    @NonNull
    public final String status;
    public final int statusType;
    @NonNull
    public final String action;

    public AdminRequest(@NonNull String id, @NonNull String citizen, @NonNull String initials,
                        @NonNull String service, @NonNull String submitted, @NonNull String officer,
                        @NonNull String status, int statusType, @NonNull String action) {
        this.id = id;
        this.citizen = citizen;
        this.initials = initials;
        this.service = service;
        this.submitted = submitted;
        this.officer = officer;
        this.status = status;
        this.statusType = statusType;
        this.action = action;
    }
}
