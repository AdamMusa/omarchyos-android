package com.google.android.gms.location;

import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcelable;

import java.util.List;

import app.grapheneos.gmscompat.UtilsKt;
import com.google.android.gms.RoSafeParcelable;

// https://developers.google.com/android/reference/com/google/android/gms/location/LocationSettingsRequest.Builder
@SafeParcelable.Class(creator = "LocationSettingsRequestCreator")
public class LocationSettingsRequest extends RoSafeParcelable {
    @Field(id = 1) public List<LocationRequest> requests;
    @Field(id = 2) public boolean alwaysShow;
    @Field(id = 3) public boolean needBle;

    @Constructor
    public LocationSettingsRequest(@Param(id = 1) List<LocationRequest> requests,
            @Param(id = 2) boolean alwaysShow, @Param(id = 3) boolean needBle) {
        this.requests = requests;
        this.alwaysShow = alwaysShow;
        this.needBle = needBle;
    }

    @Override
    public String toString() {
        return UtilsKt.objectToString(this);
    }

    public static final Parcelable.Creator<LocationSettingsRequest> CREATOR = new LocationSettingsRequestCreator();
}
