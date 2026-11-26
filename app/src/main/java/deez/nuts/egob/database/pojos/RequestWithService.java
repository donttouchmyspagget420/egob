package deez.nuts.egob.database.pojos;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

import deez.nuts.egob.database.entities.Request;

public class RequestWithService {

    @Embedded
    public Request request;

    @ColumnInfo(name = "service_name")
    public String serviceName;

    @ColumnInfo(name = "service_authority")
    public String serviceAuthority;

    @ColumnInfo(name = "service_category")
    public String serviceCategory;

    @ColumnInfo(name = "service_time")
    public String serviceTime;

    @ColumnInfo(name = "citizen_name")
    public String citizenName;
}
