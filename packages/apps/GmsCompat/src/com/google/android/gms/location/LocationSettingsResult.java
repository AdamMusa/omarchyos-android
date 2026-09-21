package com.google.android.gms.location;

import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;

// https://developers.google.com/android/reference/com/google/android/gms/location/LocationSettingsResult
@SafeParcelable.Class(creator = "LocationSettingsResultCreator")
public class LocationSettingsResult extends AbstractSafeParcelable implements Result {
    @Field(id = 1) public Status status;
    @Field(id = 2) public LocationSettingsStates settings;

    @Constructor
    public LocationSettingsResult(@Param(id = 1) Status status,
                                  @Param(id = 2) LocationSettingsStates settings) {
        this.status = status;
        this.settings = settings;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        LocationSettingsResultCreator.writeToParcel(this, dest, flags);
    }

    public static final Parcelable.Creator<LocationSettingsResult> CREATOR = null;
}
