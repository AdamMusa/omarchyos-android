package com.google.android.gms.location;

import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcelable;

import com.google.android.gms.RoSafeParcelable;

// https://developers.google.com/android/reference/com/google/android/gms/location/LastLocationRequest
@SafeParcelable.Class(creator = "LastLocationRequestCreator")
public class LastLocationRequest extends RoSafeParcelable {
    @Field(id = 1, defaultValueUnchecked = "Long.MAX_VALUE") public long maxAge;
    @Field(id = 2, defaultValueUnchecked = "LocationRequest.GRANULARITY_PERMISSION_LEVEL") public int granularity;
//    @Field(id = 3) public boolean locationSettingsIgnored; // also named "bypass"
//    @Field(id = 4) public String moduleId;
//    @Field(id = 5) public ClientIdentity impersonation;

    @Constructor
    public LastLocationRequest(@Param(id = 1) long maxAge, @Param(id = 2) int granularity) {
        this.maxAge = maxAge;
        this.granularity = granularity;
    }

    public static final Parcelable.Creator<LastLocationRequest> CREATOR = new LastLocationRequestCreator();
}
