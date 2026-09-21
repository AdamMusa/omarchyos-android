package com.google.android.gms.location.internal;

import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcelable;

import com.google.android.gms.location.LocationRequest;

import com.google.android.gms.RoSafeParcelable;

@SafeParcelable.Class(creator = "LocationRequestInternalCreator")
public class LocationRequestInternal extends RoSafeParcelable {
    @Field(id = 1) public LocationRequest request;

    /*
    these fields are never set by the recent GMS client versions

    @Field(id = 5)  public List<ClientIdentity> clients;
    @Field(id = 6)  public String tag;
    @Field(id = 7)  public boolean hideAppOps;
    @Field(id = 8)  public boolean forceCoarseLocation;
    @Field(id = 9)  public boolean exemptFromBackgroundThrottle;
    @Field(id = 10) public String moduleId;
    @Field(id = 11) public boolean locationSettingsIgnored;
     */

    @Field(id = 13) public String contextAttributionTag;
    /*
    these fields aren't necessary

    @Field(id = 12) public boolean inaccurateLocationsDelayed;
    @Field(id = 14) public long maxLocationAgeMillis;
    */

    @Constructor
    public LocationRequestInternal(@Param(id = 1) LocationRequest request, @Param(id = 13) String contextAttributionTag) {
        this.request = request;
        this.contextAttributionTag = contextAttributionTag;
    }

    public static final Parcelable.Creator<LocationRequestInternal> CREATOR = new LocationRequestInternalCreator();
}
