package com.google.android.gms.location.internal;

import android.app.PendingIntent;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.IBinder;
import android.os.Parcelable;

import com.google.android.gms.RoSafeParcelable;

@SafeParcelable.Class(creator = "LocationReceiverCreator")
public class LocationReceiver extends RoSafeParcelable {
    public static final int TYPE_ILocationListener = 1;
    public static final int TYPE_ILocationCallback = 2;
    public static final int TYPE_PendingIntent = 3;
    public static final int TYPE_ILocationStatusCallback = 4;
    public static final int TYPE_ILocationAvailabilityStatusCallback = 5;

    @Field(id = 1) public int type;
//    @Field(id = 2) public IBinder;
    @Field(id = 3) public IBinder binder;
    @Field(id = 4) public PendingIntent pendingIntent;
//    @Field(id = 5) public String;
//    @Field(id = 6) public String;

    @Constructor
    public LocationReceiver(@Param(id = 1) int type, @Param(id = 3) IBinder binder, @Param(id = 4) PendingIntent pendingIntent) {
        this.type = type;
        this.binder = binder;
        this.pendingIntent = pendingIntent;
    }

    public static final Parcelable.Creator<LocationReceiver> CREATOR = new LocationReceiverCreator();
}
