package com.google.android.gms.location.internal;

import android.annotation.Nullable;
import android.app.PendingIntent;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.IBinder;
import android.os.Parcelable;

import com.google.android.gms.location.ILocationCallback;
import com.google.android.gms.location.ILocationListener;

import com.google.android.gms.RoSafeParcelable;

@SafeParcelable.Class(creator = "LocationRequestUpdateDataCreator")
public class LocationRequestUpdateData extends RoSafeParcelable {
    public static final int OP_REQUEST_UPDATES = 1;
    public static final int OP_REMOVE_UPDATES = 2;

    @Field(id = 1) public int opCode;
    @Field(id = 2) public LocationRequestInternal request;
    @Field(id = 3) public IBinder listener;
    @Field(id = 4) public PendingIntent pendingIntent;
    @Field(id = 5) public IBinder callback;
    @Field(id = 6) public IBinder fusedLocationProviderCallback;
    @Field(id = 8) public String appOpsReasonMessage;

    @Constructor
    public LocationRequestUpdateData(@Param(id = 1) int opCode,
            @Param(id = 2) LocationRequestInternal request,
            @Param(id = 3) IBinder listener,
            @Param(id = 4) PendingIntent pendingIntent,
            @Param(id = 5) IBinder callback,
            @Param(id = 6) IBinder fusedLocationProviderCallback,
            @Param(id = 8) String appOpsReasonMessage) {
        this.opCode = opCode;
        this.request = request;
        this.listener = listener;
        this.pendingIntent = pendingIntent;
        this.callback = callback;
        this.fusedLocationProviderCallback = fusedLocationProviderCallback;
        this.appOpsReasonMessage = appOpsReasonMessage;
    }

    @Nullable
    public ILocationListener getListener() {
        return ILocationListener.Stub.asInterface(listener);
    }

    @Nullable
    public ILocationCallback getCallback() {
        return ILocationCallback.Stub.asInterface(callback);
    }

    @Nullable
    public IFusedLocationProviderCallback getFusedLocationProviderCallback() {
        return IFusedLocationProviderCallback.Stub.asInterface(fusedLocationProviderCallback);
    }

    public static final Parcelable.Creator<LocationRequestUpdateData> CREATOR = new LocationRequestUpdateDataCreator();
}
