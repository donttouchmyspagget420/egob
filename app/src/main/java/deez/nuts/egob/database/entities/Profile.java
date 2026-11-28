package deez.nuts.egob.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "profiles")
public class Profile {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String dni;
    public String phone;
    public String adress;
    public String code;
    public String fullName;

    @ColumnInfo(name = "date_of_birth")
    public String dateOfBirth;

    public String city;
    public String district;

    @ColumnInfo(name = "marital_status")
    public String maritalStatus;

    @ColumnInfo(name = "polling_place")
    public String pollingPlace;

    public String tier;

    @ColumnInfo(name = "member_since")
    public String memberSince;

    public boolean verified;

    @ColumnInfo(name = "biometric_enabled")
    public boolean biometricEnabled;

    @ColumnInfo(name = "qr_enabled")
    public boolean qrEnabled;

    @ColumnInfo(name = "access_key_enabled")
    public boolean accessKeyEnabled;

    @ColumnInfo(name = "two_factor_enabled")
    public boolean twoFactorEnabled;

    @ColumnInfo(name = "emergency_sms")
    public boolean emergencySms;

    @ColumnInfo(name = "tax_push")
    public boolean taxPush;

    @ColumnInfo(name = "health_alerts")
    public boolean healthAlerts;

    public String language;

    @ColumnInfo(name = "avatar_initials")
    public String avatarInitials;
}
