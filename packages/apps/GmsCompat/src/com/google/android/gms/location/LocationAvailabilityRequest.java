package com.google.android.gms.location;

import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcelable;

import com.google.android.gms.RoSafeParcelable;

@SafeParcelable.Class(creator = "LocationAvailabilityRequestCreator")
public class LocationAvailabilityRequest extends RoSafeParcelable {
//    @Field(id = 1) public boolean bypass;
//    @Field(id = 2) public ClientIdentity impersonation;

    @Constructor
    public LocationAvailabilityRequest() {
    }

    public static final Parcelable.Creator<LocationAvailabilityRequest> CREATOR = new LocationAvailabilityRequestCreator();
}
