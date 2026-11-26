package deez.nuts.egob.data;

import androidx.annotation.NonNull;

public class Registration {
    @NonNull
    public final String nin;
    @NonNull
    public final String fullName;
    @NonNull
    public final String dateOfBirth;
    @NonNull
    public final String phone;
    @NonNull
    public final String district;
    @NonNull
    public final String password;

    public Registration(@NonNull String nin, @NonNull String fullName, @NonNull String dateOfBirth,
                        @NonNull String phone, @NonNull String district, @NonNull String password) {
        this.nin = nin;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.phone = phone;
        this.district = district;
        this.password = password;
    }
}
