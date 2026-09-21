package com.google.android.gms.location;

import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;

// https://developers.google.com/android/reference/com/google/android/gms/location/LocationSettingsStates
@SafeParcelable.Class(creator = "LocationSettingsStatesCreator")
public class LocationSettingsStates extends AbstractSafeParcelable {
    @Field(id = 1) public boolean gpsUsable;
    @Field(id = 2) public boolean networkLocationUsable;
    @Field(id = 3) public boolean bleUsable;
    @Field(id = 4) public boolean gpsPresent;
    @Field(id = 5) public boolean networkLocationPresent;
    @Field(id = 6) public boolean blePresent;

    public LocationSettingsStates() {}

    @Constructor
    public LocationSettingsStates(@Param(id = 1) boolean gpsUsable,
            @Param(id = 2) boolean networkLocationUsable,
            @Param(id = 3) boolean bleUsable, @Param(id = 4) boolean gpsPresent,
            @Param(id = 5) boolean networkLocationPresent, @Param(id = 6) boolean blePresent) {
        this.gpsUsable = gpsUsable;
        this.networkLocationUsable = networkLocationUsable;
        this.bleUsable = bleUsable;
        this.gpsPresent = gpsPresent;
        this.networkLocationPresent = networkLocationPresent;
        this.blePresent = blePresent;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        LocationSettingsStatesCreator.writeToParcel(this, dest, flags);
    }
    
    public static final Parcelable.Creator<LocationSettingsStates> CREATOR = null;
}
