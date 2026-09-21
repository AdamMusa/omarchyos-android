package com.google.android.gms.common;

import android.app.PendingIntent;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;

import app.grapheneos.gmscompat.UtilsKt;

// https://developers.google.com/android/reference/com/google/android/gms/common/ConnectionResult
@SafeParcelable.Class(creator = "ConnectionResultCreator")
public class ConnectionResult extends AbstractSafeParcelable {
    @Field(id = 1) public int unknown1;
    @Field(id = 2) public int statusCode;
    @Field(id = 3) public PendingIntent resolution;
    @Field(id = 4) public String message;

    public static final ConnectionResult SUCCESS = new ConnectionResult(0, 0, null, null);

    @Constructor
    public ConnectionResult(@Param(id = 1) int unknown1, @Param(id = 2) int statusCode,
            @Param(id = 3) PendingIntent resolution, @Param(id = 4) String message) {
        this.unknown1 = unknown1;
        this.statusCode = statusCode;
        this.resolution = resolution;
        this.message = message;
    }

    @Override
    public String toString() {
        return UtilsKt.objectToString(this);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        ConnectionResultCreator.writeToParcel(this, dest, flags);
    }

    public static final Parcelable.Creator<ConnectionResult> CREATOR = null;
}
