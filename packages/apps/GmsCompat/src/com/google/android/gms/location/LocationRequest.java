package com.google.android.gms.location;

import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcelable;
import android.os.WorkSource;

import com.google.android.gms.RoSafeParcelable;

import app.grapheneos.gmscompat.UtilsKt;

// https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest
@SafeParcelable.Class(creator = "LocationRequestCreator")
public class LocationRequest extends RoSafeParcelable {
    public static final int PRIORITY_BALANCED_POWER_ACCURACY = 102;
    public static final int PRIORITY_HIGH_ACCURACY = 100;
    public static final int PRIORITY_LOW_POWER = 104;
    public static final int PRIORITY_NO_POWER = 105;

    // https://developers.google.com/android/reference/com/google/android/gms/location/Granularity
    public static final int GRANULARITY_PERMISSION_LEVEL = 0;
    public static final int GRANULARITY_COARSE = 1;
    public static final int GRANULARITY_FINE = 2;

    public static final int THROTTLE_BACKGROUND = 0;
    public static final int THROTTLE_ALWAYS = 1;
    public static final int THROTTLE_NEVER = 2;

    @Field(id = 1) public int priority;
    @Field(id = 2) public long interval;
    @Field(id = 3) public long minUpdateIntervalMillis;
    @Field(id = 5, defaultValueUnchecked = "Long.MAX_VALUE") public long expirationTime;
    @Field(id = 6) public int maxUpdates;
    @Field(id = 7) public float minUpdateDistanceMeters;
    @Field(id = 8) public long maxUpdateDelayMillis;
    @Field(id = 9) public boolean waitForAccurateLocation;
    @Field(id = 10, defaultValueUnchecked = "Long.MAX_VALUE") public long durationMillis;
    @Field(id = 11, defaultValue = "-1") public long maxUpdateAgeMillis;
    @Field(id = 12, defaultValueUnchecked = "LocationRequest.GRANULARITY_PERMISSION_LEVEL") public int granularity;
    @Field(id = 13) public int throttleBehavior;
    @Field(id = 15) public boolean bypass;
    @Field(id = 16) public WorkSource workSource;

    @Constructor
    public LocationRequest(@Param(id = 1) int priority,
            @Param(id = 2) long interval,
            @Param(id = 3) long minUpdateIntervalMillis,
            @Param(id = 5) long expirationTime,
            @Param(id = 6) int maxUpdates,
            @Param(id = 7) float minUpdateDistanceMeters,
            @Param(id = 8) long maxUpdateDelayMillis,
            @Param(id = 9) boolean waitForAccurateLocation,
            @Param(id = 10) long durationMillis,
            @Param(id = 11) long maxUpdateAgeMillis,
            @Param(id = 12) int granularity,
            @Param(id = 13) int throttleBehavior,
            @Param(id = 15) boolean bypass,
            @Param(id = 16) WorkSource workSource
    ) {
        this.priority = priority;
        this.interval = interval;
        this.minUpdateIntervalMillis = minUpdateIntervalMillis;
        this.expirationTime = expirationTime;
        this.maxUpdates = maxUpdates;
        this.minUpdateDistanceMeters = minUpdateDistanceMeters;
        this.maxUpdateDelayMillis = maxUpdateDelayMillis;
        this.waitForAccurateLocation = waitForAccurateLocation;
        this.durationMillis = durationMillis;
        this.maxUpdateAgeMillis = maxUpdateAgeMillis;
        this.granularity = granularity;
        this.throttleBehavior = throttleBehavior;
        this.bypass = bypass;
        this.workSource = workSource;
    }

    @Override
    public String toString() {
        return UtilsKt.objectToString(this);
    }

    public static final Parcelable.Creator<LocationRequest> CREATOR = new LocationRequestCreator();
}
