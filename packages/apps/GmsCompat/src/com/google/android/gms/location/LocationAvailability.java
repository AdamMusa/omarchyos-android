package com.google.android.gms.location;

import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;

// https://developers.google.com/android/reference/com/google/android/gms/location/LocationAvailability
@SafeParcelable.Class(creator = "LocationAvailabilityCreator")
public class LocationAvailability extends AbstractSafeParcelable {
    /*
    @Deprecated
    @Field(id = 1) public int cellStatus;
    @Deprecated
    @Field(id = 2) public int wifiStatus;
    @Field(id = 3) public long elapsedRealtimeNs;
     */
    @Field(id = 4) public int locationStatus;
    // @Deprecated @Field(id = 5) public NetworkLocationStatus[]
    @Field(id = 6) public boolean isAvailable;

    private static final int STATUS_AVAILABLE = 0;
    private static final int STATUS_UNAVAILABLE = 1000;

    @Constructor
    LocationAvailability(@Param(id = 4) int locationStatus, @Param(id = 6) boolean isAvailable) {
        this.locationStatus = locationStatus;
        this.isAvailable = isAvailable;
    }

    private static final LocationAvailability AVAILABLE = new LocationAvailability(STATUS_AVAILABLE, true);
    private static final LocationAvailability UNAVAILABLE = new LocationAvailability(STATUS_UNAVAILABLE, false);

    public static LocationAvailability get(boolean available) {
        return available? AVAILABLE : UNAVAILABLE;
    }

    public boolean isLocationAvailable() {
        return locationStatus < STATUS_UNAVAILABLE;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        LocationAvailabilityCreator.writeToParcel(this, dest, flags);
    }
    
    public static final Parcelable.Creator<LocationAvailability> CREATOR = null;
}
