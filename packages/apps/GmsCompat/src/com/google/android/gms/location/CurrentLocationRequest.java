package com.google.android.gms.location;

import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcelable;

import com.google.android.gms.RoSafeParcelable;

// https://developers.google.com/android/reference/com/google/android/gms/location/CurrentLocationRequest
@SafeParcelable.Class(creator = "CurrentLocationRequestCreator")
public class CurrentLocationRequest extends RoSafeParcelable {
    @Field(id = 1, defaultValue = "60000") public long maxUpdateAgeMillis;
    @Field(id = 2, defaultValueUnchecked = "LocationRequest.GRANULARITY_PERMISSION_LEVEL")  public int granularity;
    @Field(id = 3, defaultValueUnchecked = "LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY") public int priority;
    @Field(id = 4, defaultValueUnchecked = "Long.MAX_VALUE") public long durationMillis;
//    @Field(id = 5) public boolean bypass;
//    @Field(id = 6) public WorkSource workSource;
//    @Field(id = 7) public int throttleBehavior;
//    @Field(id = 8) public String moduleId;
//    @Field(id = 9) public ClientIdentity impersonation;

    @Constructor
    public CurrentLocationRequest(@Param(id = 1) long maxUpdateAgeMillis, @Param(id = 2) int granularity,
                                  @Param(id = 3) int priority, @Param(id = 4) long durationMillis) {
        this.maxUpdateAgeMillis = maxUpdateAgeMillis;
        this.granularity = granularity;
        this.priority = priority;
        this.durationMillis = durationMillis;
    }

    public static final Parcelable.Creator<CurrentLocationRequest> CREATOR = new CurrentLocationRequestCreator();
}
