package com.google.android.gms.common.api;

import android.app.PendingIntent;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.common.ConnectionResult;

import app.grapheneos.gmscompat.UtilsKt;

// https://developers.google.com/android/reference/com/google/android/gms/common/api/Status
@SafeParcelable.Class(creator = "StatusCreator")
public final class Status extends AbstractSafeParcelable implements Result {
    public static final Status SUCCESS = new Status(CommonStatusCodes.SUCCESS, null, null, ConnectionResult.SUCCESS);

    @Field(id = 1) public int statusCode;
    @Field(id = 2) public String statusMessage;
    @Field(id = 3) public PendingIntent resolution;
    @Field(id = 4) public ConnectionResult connectionResult;

    @Constructor
    public Status(@Param(id = 1) int statusCode, @Param(id = 2) String statusMessage,
            @Param(id = 3) PendingIntent resolution, @Param(id = 4) ConnectionResult connectionResult) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.resolution = resolution;
        this.connectionResult = connectionResult;
    }

    public Status(int statusCode) {
        this(statusCode, null, null, null);
    }

    @Override
    public Status getStatus() {
        return this;
    }

    public boolean isSuccess() {
        // there is SUCCESS (0) and SUCCESS_CACHE (-1)
        return statusCode <= CommonStatusCodes.SUCCESS;
    }

    @Override
    public String toString() {
        return UtilsKt.objectToString(this);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        StatusCreator.writeToParcel(this, dest, flags);
    }

    public static final Parcelable.Creator<Status> CREATOR = null;
}
