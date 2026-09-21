package com.google.android.gms.location;

import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

// https://developers.google.com/android/reference/com/google/android/gms/location/LocationResult
@SafeParcelable.Class(creator = "LocationResultCreator")
public class LocationResult extends AbstractSafeParcelable {
    @Field(id = 1) public final List<Location> locations;

    @Constructor
    public LocationResult(@Param(id = 1) List<Location> locations) {
        this.locations = locations;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        LocationResultCreator.writeToParcel(this, dest, flags);
    }

    public static final Parcelable.Creator<LocationResult> CREATOR = null;
}
